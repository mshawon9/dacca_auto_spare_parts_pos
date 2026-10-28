$(function () {
    const root = '/purchase-orders/invoice';
    let image = null;
    let imageUrl = null;
    let rows = [];
    let working = false;
    let references = null;
    let productRow = null;
    let modalBusy = false;
    let phone = null;
    let phoneTimer = null;
    let invoiceData = null;
    const modalElement = document.getElementById('invoiceNewProductModal');
    const productModal = bootstrap.Modal.getOrCreateInstance(modalElement);
    $(modalElement).appendTo(document.body);
    const message = (text, error) => $('#invoiceImportMessage').text(text)
        .toggleClass('alert-danger', !!error).toggleClass('alert-light', !error);
    const productMessage = text => $('#invoiceProductMessage').text(text).removeClass('d-none');

    async function api(url, options) {
        const response = await fetch(url, Object.assign({cache: 'no-store'}, options));
        const body = await response.text();
        let data;
        try { data = body ? JSON.parse(body) : null; } catch (_) { data = null; }
        if (!response.ok) {
            const fields = data && data.fieldErrors ? Object.values(data.fieldErrors).join(' ') : '';
            const error = new Error(fields || (data && (data.error || data.message || data.detail)) ||
                (response.status === 413 ? 'Image is too large. Choose a smaller JPEG/PNG image.' : 'Request failed. Please try again.'));
            error.status = response.status;
            throw error;
        }
        return data;
    }
    const post = (url, body) => api(url, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body)});
    function busy(value) {
        working = value;
        $('#invoiceExtract, #invoiceDemo, #invoiceChooseImage, #invoiceTakePhoto, #invoiceConnectPhone, #invoiceMatchAll, #invoiceDiscard').prop('disabled', value);
        $('#invoiceExtract').text(value ? 'Processing...' : 'Extract with AI');
    }
    const money = value => Number(value || 0).toFixed(2);
    function lineValue(row, field) {
        const input = row.find('[data-field="' + field + '"]');
        return input.length ? String(input.val() || '').trim() : String((row.data('extracted-line') || {})[field] || '').trim();
    }
    function query(row) {
        return {name: lineValue(row, 'name'), partNumber: lineValue(row, 'partNumber'),
            sku: lineValue(row, 'sku'), brand: lineValue(row, 'brand')};
    }
    function destroyRows() {
        $('#purchaseLines .invoice-product-select').each(function () {
            if ($(this).hasClass('select2-hidden-accessible')) $(this).select2('destroy');
        });
        rows = [];
        $('#purchaseLines tr[data-invoice-source]').remove();
        invoiceData = null;
        $('#invoiceSupplierSuggestion').prop('hidden', true);
        $(document).trigger('purchase:refresh-lines');
    }
    function setRowStatus(row, text, success) {
        row.toggleClass('invoice-resolved', !!success);
        row.find('.invoice-match-status').text(text).toggleClass('text-bg-success', !!success)
            .toggleClass('text-bg-warning', !success);
    }
    function liveRows() { return rows.filter(row => row.closest(document.documentElement).length); }
    function totals() {
        if (!invoiceData) return;
        const actual = Number($('#grandTotal').text()) || 0;
        const printed = invoiceData.total;
        const mismatch = printed != null && Math.abs(actual - Number(printed)) > 0.02;
        $('#invoiceTotalCheck').text('PO total: ' + money(actual) + (printed == null ? '' :
            ' | Printed invoice: ' + money(printed)) + (mismatch ? ' - Check VAT, missing items and discounts.' : ''))
            .toggleClass('text-danger', mismatch);
    }
    function renderLine(row, index) {
        const cell = row.find('td').first();
        const resolution = $('<div class="invoice-line-resolution mt-2"></div>').appendTo(cell);
        $('<span class="badge text-bg-warning invoice-match-status mb-1">Checking product...</span>').appendTo(resolution);
        const choice = $('<select class="form-select invoice-product-select"><option value=""></option></select>').appendTo(resolution)
            .attr('aria-label', 'Choose product for invoice item ' + (index + 1));
        const buttons = $('<div class="d-flex gap-2 mt-1"></div>').appendTo(resolution);
        $('<button type="button" class="btn btn-sm btn-outline-secondary invoice-recheck">Recheck</button>').appendTo(buttons);
        $('<button type="button" class="btn btn-sm btn-outline-primary invoice-new-product">New Product</button>').appendTo(buttons);
        $('<button type="button" class="btn btn-sm btn-link invoice-change-product">Change product</button>').appendTo(buttons)
            .on('click', function () { row.toggleClass('invoice-editing'); });
        choice.select2({
            theme: 'bootstrap-5', width: '100%', allowClear: true, placeholder: 'Choose or search product', minimumInputLength: 0,
            ajax: {url: '/purchase-orders/product-search', dataType: 'json', delay: 300,
                data: params => ({keyword: params.term || ''}),
                transport: function (params, success, failure) {
                    if ((params.data.keyword || '').length < 2) {
                        success({local: true}); return {abort: function () {}};
                    }
                    const request = $.ajax(params); request.then(success, failure); return request;
                },
                processResults: items => ({results: items && items.local ?
                    choice.find('option').filter(function () { return !!this.value; }).map(function () { return {id: this.value, text: this.text}; }).get() : items || []})}
        });
        choice.on('change', function () {
            const selected = choice.select2('data')[0];
            row.removeClass('invoice-editing');
            row.find('.line-product-id').val(selected && selected.id ? selected.id : '');
            if (selected && selected.id) {
                row.find('.product-label').text(selected.text);
                row.find('.line-product-text').val(selected.text);
            }
            setRowStatus(row, this.value ? 'Product selected' : 'Choose product', !!this.value);
            $('#invoiceReviewed').prop('checked', false);
            $(document).trigger('purchase:refresh-lines');
        });
        row.find('.invoice-recheck').on('click', () => matchRows([row]));
        row.find('.invoice-new-product').on('click', () => openProduct(row));
        if (row.find('.line-tax-percent').val() === '') {
            row.addClass('is-expanded').find('.line-options-toggle').attr('aria-expanded', 'true').text('Hide details');
        }
    }
    async function matchRows(targets) {
        if (working || !targets.length) return;
        targets = targets.filter(row => {
            const values = query(row);
            const valid = Object.entries(values).every(([key, value]) => value.length <= (key === 'name' ? 200 : 100));
            if (!valid) setRowStatus(row, 'Search or create product to resolve this item', false);
            return valid;
        });
        if (!targets.length) return;
        busy(true);
        const queries = targets.map(query);
        try {
            const matches = await post(root + '/match', {lines: queries});
            matches.forEach((match, index) => {
                const row = targets[index];
                if (!row.closest(document.documentElement).length || JSON.stringify(query(row)) !== JSON.stringify(queries[index])) return;
                const select = row.find('.invoice-product-select').empty().append(new Option('', ''));
                match.candidates.forEach(candidate => select.append(new Option(candidate.text, candidate.id)));
                if (match.status === 'MATCHED') {
                    select.val(String(match.candidates[0].id)).trigger('change');
                    setRowStatus(row, 'Matched - ' + match.candidates[0].reason, true);
                } else {
                    select.val(null).trigger('change');
                    setRowStatus(row, match.status === 'NOT_FOUND' ? 'Not found - search or create' :
                        (match.candidates.length === 1 ? 'Name suggestion - confirm product' : 'Possible matches - choose product / brand'), false);
                }
            });
        } catch (error) {
            targets.forEach(row => {
                if (!row.find('.line-product-id').val()) setRowStatus(row, 'Match unavailable - search or recheck', false);
            });
            message(error.message, true);
        }
        finally { busy(false); }
    }
    function suggestSupplier(name) {
        const normalized = window.normalizePurchaseSupplierName(name);
        const matches = $('#supplierId option').filter(function () {
            return this.value && normalized && window.normalizePurchaseSupplierName(this.text) === normalized;
        });
        $('#invoiceSupplierSuggestion').prop('hidden', matches.length === 1);
        $('#invoiceSupplierMatchMessage').text(matches.length > 1 ? 'Multiple suppliers match; select the correct one:' : 'Supplier not found:');
        $('#invoiceAddSuggestedSupplier').prop('hidden', matches.length > 1);
        if (matches.length === 1) {
            $('#supplierId').val(matches.val()).trigger('change.select2');
        } else {
            $('#supplierId').val(null).trigger('change.select2');
            $('#invoiceSuggestedSupplierName').text(name || 'Supplier name was not readable');
            $('#quickSupplierName').val(name || '');
        }
    }
    const supplierFields = {
        Name: 'supplierName', ContactPerson: 'supplierContactPerson', Phone: 'supplierPhone',
        Email: 'supplierEmail', Address: 'supplierAddress', TrnNumber: 'supplierTrnNumber'
    };
    function prefillSupplier(onlyEmpty) {
        if (!invoiceData) return;
        Object.entries(supplierFields).forEach(([field, source]) => {
            const input = $('#quickSupplier' + (field === 'TrnNumber' ? 'Trn' : field));
            if (!onlyEmpty || !input.val()) input.val(invoiceData[source] || '');
        });
    }
    async function review(data, demo) {
        const manualRows = $('#purchaseLines tr').not('[data-invoice-source]');
        const supplierName = (data.supplierName || '').trim();
        const currentSupplier = $('#supplierId option:selected').text().trim();
        const invoiceId = (data.invoiceId || '').trim();
        const currentId = $('#invoiceId').val().trim();
        if (manualRows.length) {
            if ((currentId && invoiceId && currentId !== invoiceId) ||
                ($('#supplierId').val() && supplierName && window.normalizePurchaseSupplierName(currentSupplier) !== window.normalizePurchaseSupplierName(supplierName))) {
                throw new Error('This PO already contains items for another invoice or supplier. Start a new PO before extracting this invoice.');
            }
            if (!confirm('Keep the existing ' + manualRows.length + ' items and add the extracted items? Check that these are not duplicate invoice lines.')) return;
        }
        destroyRows();
        invoiceData = data;
        Object.entries(supplierFields).forEach(([field, source]) => {
            $('#invoiceSupplier' + field).val(data[source] || '');
            $('#invoiceSupplierDisplay' + field).text(data[source] || '-');
        });
        $('#invoiceSupplierSnapshot').prop('hidden', !data.supplierName);
        $('#invoiceId').val(invoiceId);
        $(document).trigger('purchase:set-date', [data.purchaseDate || '']);
        suggestSupplier(supplierName);
        const warnings = (data.warnings || []).slice();
        if (data.currency) warnings.push('Invoice currency: ' + data.currency + '. Verify this is the currency used by your PO.');
        if (demo) warnings.unshift('SAMPLE DEMO: these are fixed sample values, not information read from your image.');
        $('#invoiceWarnings').empty();
        warnings.forEach(warning => $('<li></li>').text(warning).appendTo('#invoiceWarnings'));
        $('#invoiceWarningSummary').text('Review extraction warnings (' + warnings.length + ')');
        $('#invoiceWarningDetails').prop('hidden', !warnings.length);
        const batch = {lines: data.lines || [], rows: []};
        $(document).trigger('purchase:load-extracted', [batch]);
        rows = batch.rows;
        rows.forEach(renderLine);
        $('#invoiceReview').prop('hidden', false);
        $('#invoiceReviewed').prop('checked', false);
        message(demo ? 'SAMPLE DEMO filled into the PO. Review the sample supplier and items below.' :
            'Invoice details and items filled into the purchase form. Review products and amounts, then save.');
        totals();
        await matchRows(liveRows());
        document.getElementById('purchaseForm').scrollIntoView({behavior: 'smooth', block: 'start'});
    }
    async function optimize(file) {
        if (!['image/jpeg', 'image/png'].includes(file.type)) throw new Error('Choose a JPEG or PNG image. Export HEIC images as JPEG first.');
        if (file.size > 20 * 1024 * 1024) throw new Error('Photo is too large. Choose an image below 20 MB before optimization.');
        if (typeof window.createImageBitmap !== 'function') throw new Error('This browser cannot prepare the image. Use an up-to-date Chrome browser.');
        const bitmap = await createImageBitmap(file);
        try {
            const ratio = Math.min(1, 2200 / Math.max(bitmap.width, bitmap.height));
            const canvas = document.createElement('canvas');
            canvas.width = Math.max(1, Math.round(bitmap.width * ratio)); canvas.height = Math.max(1, Math.round(bitmap.height * ratio));
            const context = canvas.getContext('2d'); context.fillStyle = '#fff'; context.fillRect(0, 0, canvas.width, canvas.height); context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
            const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.9));
            if (!blob || blob.size > 3 * 1024 * 1024) throw new Error('Image exceeds 3 MB. Choose a smaller image.');
            return new File([blob], 'supplier-invoice.jpg', {type: 'image/jpeg'});
        } finally { bitmap.close(); }
    }
    async function chooseImage(file) {
        if (!file || working) return;
        if (rows.length && !confirm('Replace the current invoice review? Items already added to the PO will remain.')) return;
        busy(true);
        try {
            image = await optimize(file);
            if (imageUrl) URL.revokeObjectURL(imageUrl);
            imageUrl = URL.createObjectURL(image);
            $('#invoiceImagePreview').attr('src', imageUrl);
            $('#invoiceImageOpen').attr('href', imageUrl);
            $('#invoiceImageName').text(file.name + ' - ' + Math.round(image.size / 1024) + ' KB');
            $('#invoiceImagePanel').removeClass('d-none');
            destroyRows(); $('#invoiceReview').prop('hidden', true);
            message('Image ready. Click Extract with AI to read this invoice.');
        } catch (error) { message(error.message || 'Could not open this image.', true); }
        finally { busy(false); }
    }
    $('#invoiceChooseImage').on('click', () => $('#invoiceImageFile').trigger('click'));
    $('#invoiceTakePhoto').on('click', () => $('#invoiceCameraFile').trigger('click'));
    $('#invoiceImageFile, #invoiceCameraFile').on('change', function () { chooseImage(this.files[0]); this.value = ''; });
    $('#invoiceExtract').on('click', async () => {
        if (!image || working) return;
        if (rows.length && !confirm('Extract again and replace the current review? This makes another AI request.')) return;
        const timeoutSeconds = Number($('#purchaseOrderPageData').data('ai-timeout-seconds')) || 180;
        const started = Date.now();
        busy(true);
        const progress = () => message('Reading the invoice... ' + Math.floor((Date.now() - started) / 1000) +
            ' seconds elapsed. Large invoices may take up to ' + timeoutSeconds + ' seconds. Please keep this page open.');
        progress();
        const progressTimer = setInterval(progress, 1000);
        try {
            const form = new FormData(); form.append('file', image);
            const data = await api(root + '/extract', {method: 'POST', body: form});
            clearInterval(progressTimer);
            busy(false); await review(data, false);
        } catch (error) { message(error.message, true); }
        finally { clearInterval(progressTimer); busy(false); }
    });
    $('#invoiceDemo').on('click', async () => {
        if (working || (rows.length && !confirm('Replace the current review with fixed sample data?'))) return;
        busy(true);
        try { const data = await post(root + '/demo', {}); busy(false); await review(data, true); }
        catch (error) { message(error.message, true); }
        finally { busy(false); }
    });
    $('#invoiceMatchAll').on('click', () => matchRows(liveRows().filter(row => !row.find('.line-product-id').val())));
    $('#invoiceId, #purchaseDate, #storeId').on('input change', () => $('#invoiceReviewed').prop('checked', false));
    $(document).on('purchase:totals-updated', totals);
    $('#purchaseLines').on('input change', 'input', function () {
        $('#invoiceReviewed').prop('checked', false);
    });
    $('#supplierId').on('change', function () {
        if (this.value) $('#invoiceSupplierSuggestion').prop('hidden', true);
        $('#invoiceReviewed').prop('checked', false);
    });
    $(document).on('purchase:supplier-created', function () {
        $('#invoiceSupplierSuggestion').prop('hidden', true);
    });
    $('#invoiceAddSuggestedSupplier').on('click', function () {
        prefillSupplier(false);
        bootstrap.Modal.getOrCreateInstance(document.getElementById('supplierQuickModal')).show();
    });
    $('#supplierQuickModal').on('show.bs.modal', function () { prefillSupplier(true); });
    $('#invoiceDiscard').on('click', () => {
        if (confirm('Remove the extracted items from this PO? Manually added items and catalog records will remain.')) {
            destroyRows();
            $('#invoiceReview, #invoiceSupplierSuggestion').prop('hidden', true);
            message('Extracted items removed. Check the invoice details before saving.');
        }
    });

    function populate(select, items, chosenName) {
        select.empty().append(new Option('Select', ''));
        items.forEach(item => select.append(new Option(item.name, item.id)));
        const match = items.find(item => item.name.trim().toLowerCase() === (chosenName || '').trim().toLowerCase());
        select.val(match ? String(match.id) : '').trigger('change');
    }
    async function openProduct(row) {
        if (working) return;
        busy(true);
        try {
            references = await api(root + '/references');
            productRow = row;
            const line = row.data('extracted-line');
            $('#invoiceNewProductForm')[0].reset();
            $('#invoiceProductMessage').addClass('d-none');
            $('#invoiceNewCategoryPanel, #invoiceNewBrandPanel').prop('hidden', true);
            $('#invoiceToggleCategory, #invoiceToggleBrand').attr('aria-expanded', 'false');
            $('#invoiceProductName').val(lineValue(row, 'name'));
            $('#invoiceProductPart').val(lineValue(row, 'partNumber').replace(/\s+/g, ''));
            $('#invoiceProductSku').val(lineValue(row, 'sku'));
            populate($('#invoiceProductCategory'), references.categories, line.category);
            populate($('#invoiceProductBrand'), references.brands, lineValue(row, 'brand'));
            $('#invoiceCategoryName').val(line.category || ''); $('#invoiceBrandName').val(lineValue(row, 'brand'));
            $('#invoiceProductPosition').empty().append(new Option('Not specified', ''));
            references.positions.forEach(p => $('#invoiceProductPosition').append(new Option(p.text, p.value)));
            productModal.show();
        } catch (error) { message(error.message, true); }
        finally { busy(false); }
    }
    $('#invoiceProductCategory, #invoiceProductBrand').select2({theme: 'bootstrap-5', width: '100%', dropdownParent: $('#invoiceNewProductModal')});
    [['Category', 'invoiceNewCategoryPanel'], ['Brand', 'invoiceNewBrandPanel']].forEach(([kind, panel]) => {
        $('#invoiceToggle' + kind).on('click', function () {
            const open = $('#' + panel).prop('hidden'); $('#' + panel).prop('hidden', !open); $(this).attr('aria-expanded', String(open));
            if (open) $('#invoice' + kind + 'Name').trigger('focus');
        });
        $('#invoice' + kind + 'Name').on('keydown', function (event) {
            if (event.key === 'Enter') { event.preventDefault(); $('#invoiceSave' + kind).trigger('click'); }
        });
        $('#invoiceSave' + kind).on('click', async function () {
            if (modalBusy) return;
            const name = $('#invoice' + kind + 'Name').val().trim();
            if (!name || name.length > 100) { productMessage('Enter a ' + kind.toLowerCase() + ' name up to 100 characters.'); return; }
            modalBusy = true; $('#invoiceNewProductForm button').prop('disabled', true);
            try {
                const items = kind === 'Brand' ? references.brands : references.categories;
                let value = items.find(item => item.name.trim().toLowerCase() === name.toLowerCase());
                if (!value) { value = await post(kind === 'Brand' ? '/brands/quick-create' : root + '/categories', {name, active: true}); items.push(value); }
                populate($('#invoiceProduct' + kind), items, value.name);
                $('#' + panel).prop('hidden', true); $('#invoiceToggle' + kind).attr('aria-expanded', 'false'); $('#invoiceProductMessage').addClass('d-none');
            } catch (error) { productMessage(error.message); }
            finally { modalBusy = false; $('#invoiceNewProductForm button').prop('disabled', false); }
        });
    });
    $('#invoiceProductPart').on('input', function () { this.value = this.value.replace(/\s+/g, ''); });
    $('#purchaseForm').on('submit', function (event) {
        if (working || (liveRows().length && !$('#invoiceReviewed').is(':checked'))) {
            event.preventDefault();
            $('#purchaseFormError').text('Review the extracted items and tick the invoice review confirmation before saving.').removeClass('d-none');
            document.getElementById('purchaseForm').scrollIntoView({behavior: 'smooth', block: 'start'});
        }
    });
    $('#invoiceNewProductForm').on('submit', async function (event) {
        event.preventDefault();
        if (modalBusy || !productRow) return;
        const name = $('#invoiceProductName').val().trim();
        const part = $('#invoiceProductPart').val().trim();
        if (!name || name.length > 200 || !/^[A-Za-z0-9._/\-]+$/.test(part) || part.length > 100 ||
            !$('#invoiceProductCategory').val() || !$('#invoiceProductBrand').val()) {
            productMessage('Enter a product name, valid part number without spaces, category and brand.');
            return;
        }
        modalBusy = true; $('#invoiceNewProductForm button').prop('disabled', true);
        try {
            const product = await post(root + '/products', {
                name: $('#invoiceProductName').val().trim(), partNumber: $('#invoiceProductPart').val().trim(), sku: $('#invoiceProductSku').val().trim() || null,
                categoryId: Number($('#invoiceProductCategory').val()) || null, brandId: Number($('#invoiceProductBrand').val()) || null,
                position: $('#invoiceProductPosition').val() || null, reorderLevel: 2, active: true
            });
            productRow.find('.invoice-product-select').append(new Option(product.text, product.id, true, true)).trigger('change');
            setRowStatus(productRow, 'New product created', true);
            modalBusy = false; productModal.hide();
            message('Product created and selected in the purchase items table. Review the remaining items before saving.');
        } catch (error) { productMessage(error.message); }
        finally { modalBusy = false; $('#invoiceNewProductForm button').prop('disabled', false); }
    });
    $(modalElement).on('hide.bs.modal', event => { if (modalBusy) event.preventDefault(); });

    async function disconnectPhone() {
        clearTimeout(phoneTimer); const previous = phone; phone = null; $('#invoicePhonePanel').prop('hidden', true);
        if (previous) await api(root + '/phone/connections/' + previous.token, {method: 'DELETE'}).catch(() => {});
    }
    async function pollPhone() {
        const current = phone;
        if (!current) return;
        try {
            if (Date.now() >= Date.parse(current.expiresAt)) throw new Error('Phone connection expired. Choose Use Phone again.');
            if (!working) {
                const response = await fetch(root + '/phone/connections/' + current.token, {cache: 'no-store'});
                if (phone !== current) return;
                if (response.status === 410 || response.status === 404) throw new Error('Phone connection expired. Choose Use Phone again.');
                if (response.status === 200) {
                    const blob = await response.blob();
                    await disconnectPhone();
                    await chooseImage(new File([blob], 'phone-invoice.jpg', {type: blob.type}));
                    return;
                }
                if (response.status !== 204) throw new Error('Phone upload could not be received. Reconnect and try again.');
            }
        } catch (error) { message(error.message, true); await disconnectPhone(); }
        finally { if (phone === current) phoneTimer = setTimeout(pollPhone, 1500); }
    }
    $('#invoiceConnectPhone').on('click', async () => {
        if (working) return;
        busy(true);
        try {
            await disconnectPhone(); phone = await post(root + '/phone/connections', {});
            const link = location.origin + root + '/phone/mobile#' + phone.token;
            $('#invoicePhoneLink').val(link); $('#invoicePhoneQr').empty();
            new QRCode(document.getElementById('invoicePhoneQr'), {text: link, width: 160, height: 160});
            $('#invoicePhonePanel').prop('hidden', false);
            message(location.hostname === 'localhost' || location.hostname === '127.0.0.1' ?
                'Localhost cannot be reached from your phone. Open this PO page using your reachable HTTPS address first.' : 'Waiting for your phone photo. Uploading does not send it to AI until you click Extract.');
        } catch (error) { message(error.message, true); }
        finally { busy(false); if (phone) pollPhone(); }
    });
    $('#invoiceDisconnectPhone').on('click', disconnectPhone);
    window.addEventListener('pagehide', () => {
        if (imageUrl) URL.revokeObjectURL(imageUrl);
        if (phone) fetch(root + '/phone/connections/' + phone.token, {method: 'DELETE', keepalive: true});
    });
});

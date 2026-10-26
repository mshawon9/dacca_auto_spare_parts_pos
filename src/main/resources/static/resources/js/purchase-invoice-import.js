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
    const modalElement = document.getElementById('invoiceNewProductModal');
    const productModal = bootstrap.Modal.getOrCreateInstance(modalElement);
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
        $('#invoiceExtract, #invoiceDemo, #invoiceChooseImage, #invoiceTakePhoto, #invoiceConnectPhone, #invoiceMatchAll, #invoiceApply, #invoiceDiscard').prop('disabled', value);
        $('#invoiceExtract').text(value ? 'Processing...' : 'Extract with AI');
    }
    const number = input => input.val() === '' ? null : Number(input.val());
    const money = value => Number(value || 0).toFixed(2);
    function lineValue(row, field) { return row.find('[data-invoice-field="' + field + '"]').val().trim(); }
    function query(row) {
        return {name: lineValue(row, 'name'), partNumber: lineValue(row, 'partNumber'),
            sku: lineValue(row, 'sku'), brand: lineValue(row, 'brand')};
    }
    function destroyRows() {
        $('#invoiceReviewLines .invoice-product-select').each(function () {
            if ($(this).hasClass('select2-hidden-accessible')) $(this).select2('destroy');
        });
        rows = [];
        $('#invoiceReviewLines').empty();
    }
    function setRowStatus(row, text, success) {
        row.find('.invoice-match-status').text(text).toggleClass('text-bg-success', !!success)
            .toggleClass('text-bg-warning', !success);
    }
    function totals() {
        let total = 0;
        rows.forEach(row => {
            if (!row.find('.invoice-include').is(':checked')) return;
            const qty = number(row.find('[data-invoice-field="quantity"]'));
            const price = number(row.find('[data-invoice-field="unitPrice"]'));
            const tax = number(row.find('[data-invoice-field="taxPercent"]'));
            total += Number((qty * price).toFixed(2)) + Number((qty * price * tax / 100).toFixed(2));
        });
        const printed = number($('#extractedTotal'));
        const mismatch = printed != null && Math.abs(total - printed) > 0.02;
        $('#invoiceTotalCheck').text('Selected items total: ' + money(total) + (printed == null ? '' :
            ' | Printed total: ' + money(printed)) + (mismatch ? ' - Totals differ. Check tax, missing items, discounts, and tax-inclusive prices.' : ''))
            .toggleClass('text-danger', mismatch);
    }
    function renderLine(line, index) {
        const row = $('<div class="border rounded p-3 mb-2 invoice-review-row"></div>');
        const top = $('<div class="d-flex flex-wrap align-items-center gap-2 mb-2"></div>').appendTo(row);
        $('<label class="form-check mb-0"><input type="checkbox" class="form-check-input invoice-include" checked><span class="form-check-label"></span></label>')
            .find('span').text('Item ' + (index + 1)).end().appendTo(top);
        $('<span class="badge text-bg-warning invoice-match-status">Not checked</span>').appendTo(top);
        const grid = $('<div class="row g-2"></div>').appendTo(row);
        [['name', 'Description', 'text', 'col-md-4'], ['partNumber', 'Part Number', 'text', 'col-md-3'],
            ['sku', 'SKU', 'text', 'col-md-2'], ['brand', 'Brand', 'text', 'col-md-3'],
            ['quantity', 'Quantity', 'number', 'col-4 col-md-2'], ['unitPrice', 'Unit Price (excl. tax)', 'number', 'col-4 col-md-3'],
            ['taxPercent', 'Tax %', 'number', 'col-4 col-md-2'], ['supplierProductCode', 'Supplier Product Code', 'text', 'col-md-5']]
            .forEach(([field, label, type, size]) => {
                const cell = $('<div></div>').addClass(size).appendTo(grid);
                const id = 'invoice-row-' + index + '-' + field;
                $('<label class="form-label small mb-1"></label>').attr('for', id).text(label).appendTo(cell);
                const input = $('<input class="form-control form-control-sm">').attr({id: id, type: type, 'data-invoice-field': field})
                    .val(line[field] == null ? '' : line[field]).appendTo(cell);
                if (type === 'number') input.attr({min: field === 'quantity' ? 1 : 0, step: field === 'quantity' ? 1 : 0.01});
                else input.attr('maxlength', field === 'name' ? 200 : 100);
            });
        const resolution = $('<div class="row g-2 align-items-end mt-1"></div>').appendTo(row);
        const choice = $('<div class="col-md-8"><label class="form-label small">Our Product - choose a match or search</label><select class="form-select invoice-product-select"><option value=""></option></select></div>').appendTo(resolution);
        const buttons = $('<div class="col-md-4 d-flex gap-2"></div>').appendTo(resolution);
        $('<button type="button" class="btn btn-sm btn-outline-secondary invoice-recheck">Recheck</button>').appendTo(buttons);
        $('<button type="button" class="btn btn-sm btn-outline-primary invoice-new-product">New Product</button>').appendTo(buttons);
        row.data('extracted-line', line).appendTo('#invoiceReviewLines');
        choice.find('label').attr('for', 'invoice-product-' + index);
        choice.find('select').attr('id', 'invoice-product-' + index).select2({
            theme: 'bootstrap-5', width: '100%', allowClear: true, placeholder: 'Choose match or search product', minimumInputLength: 0,
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
        choice.find('select').on('change', function () { setRowStatus(row, this.value ? 'Product selected' : 'Choose product', !!this.value); $('#invoiceReviewed').prop('checked', false); });
        row.on('input', '[data-invoice-field]', function () {
            $('#invoiceReviewed').prop('checked', false);
            if (['name', 'partNumber', 'sku', 'brand'].includes($(this).data('invoice-field'))) {
                choice.find('select').empty().append(new Option('', '')).val(null).trigger('change');
                setRowStatus(row, 'Changed - recheck match', false);
            }
            totals();
        });
        row.on('change', '.invoice-include', function () { $('#invoiceReviewed').prop('checked', false); totals(); });
        row.find('.invoice-recheck').on('click', () => matchRows([row]));
        row.find('.invoice-new-product').on('click', () => openProduct(row));
        return row;
    }
    async function matchRows(targets) {
        if (working || !targets.length) return;
        targets = targets.filter(row => {
            const values = query(row);
            const valid = Object.entries(values).every(([key, value]) => value.length <= (key === 'name' ? 200 : 100));
            if (!valid) setRowStatus(row, 'Text too long - correct fields and recheck', false);
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
                match.candidates.forEach(candidate => select.append(new Option(candidate.text + ' (' + candidate.reason + ')', candidate.id)));
                if (match.status === 'MATCHED') {
                    select.val(String(match.candidates[0].id)).trigger('change');
                    setRowStatus(row, 'Matched - ' + match.candidates[0].reason, true);
                } else {
                    select.val(null).trigger('change');
                    setRowStatus(row, match.status === 'NOT_FOUND' ? 'Not found - search or create' : 'Multiple / possible match - choose product', false);
                }
            });
        } catch (error) { message(error.message, true); }
        finally { busy(false); }
    }
    async function review(data, demo) {
        destroyRows();
        $('#extractedSupplier').val(data.supplierName || '');
        $('#extractedInvoiceId').val(data.invoiceId || '');
        $('#extractedDate').val(data.purchaseDate || '');
        $('#extractedTotal').val(data.total == null ? '' : data.total);
        const warnings = (data.warnings || []).slice();
        if (data.currency) warnings.push('Invoice currency: ' + data.currency + '. Verify this is the currency used by your PO.');
        $('#invoiceWarnings').empty();
        warnings.forEach(warning => $('<li></li>').text(warning).appendTo('#invoiceWarnings'));
        $('#invoiceWarningSummary').text('Review extraction warnings (' + warnings.length + ')');
        $('#invoiceWarningDetails').prop('hidden', !warnings.length);
        rows = (data.lines || []).map(renderLine);
        $('#invoiceReview').prop('hidden', false);
        $('#invoiceReviewed').prop('checked', false);
        message(demo ? 'SAMPLE DEMO: fixed example data, not information extracted from your image. No AI request was made.' : 'Extraction ready. Review every field and resolve products before adding to the PO.');
        totals();
        await matchRows(rows);
    }
    async function optimize(file) {
        if (!['image/jpeg', 'image/png'].includes(file.type)) throw new Error('Choose a JPEG or PNG image. Export HEIC images as JPEG first.');
        if (file.size > 20 * 1024 * 1024) throw new Error('Photo is too large. Choose an image below 20 MB before optimization.');
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
    $('#invoiceMatchAll').on('click', () => matchRows(rows));
    $('#extractedTotal').on('input', totals);
    $('#extractedSupplier, #extractedInvoiceId, #extractedDate, #extractedTotal').on('input', () => $('#invoiceReviewed').prop('checked', false));
    $('#invoiceDiscard').on('click', () => {
        if (confirm('Discard the extracted review? Products and items already saved or added will remain.')) {
            destroyRows(); $('#invoiceReview').prop('hidden', true); message('Review discarded.');
        }
    });
    $('#invoiceApply').on('click', function () {
        try {
            if (working) return;
            if (!$('#invoiceReviewed').is(':checked')) throw new Error('Check the invoice and tick the review confirmation first.');
            if (!$('#storeId').val()) throw new Error('Select the warehouse before adding invoice items.');
            const lines = rows.filter(row => row.find('.invoice-include').is(':checked')).map((row, index) => {
                const selected = row.find('.invoice-product-select').select2('data')[0];
                const quantity = number(row.find('[data-invoice-field="quantity"]'));
                const unitPrice = number(row.find('[data-invoice-field="unitPrice"]'));
                const taxPercent = number(row.find('[data-invoice-field="taxPercent"]'));
                if (!selected || !selected.id) throw new Error('Select or create a product for included item ' + (index + 1) + '.');
                if (!Number.isSafeInteger(quantity) || quantity < 1) throw new Error('Quantity must be a whole number greater than zero for item ' + (index + 1) + '.');
                if (unitPrice == null || !Number.isFinite(unitPrice) || unitPrice <= 0) throw new Error('Enter a unit price greater than zero for item ' + (index + 1) + '.');
                if (taxPercent == null || !Number.isFinite(taxPercent) || taxPercent < 0 || taxPercent > 100) throw new Error('Enter tax between 0 and 100 for item ' + (index + 1) + '.');
                if ([unitPrice, taxPercent].some(value => Math.abs(value * 100 - Math.round(value * 100)) > 0.000001)) throw new Error('Unit price and tax must have at most 2 decimals for item ' + (index + 1) + '.');
                return {product: {id: selected.id, text: selected.text}, quantity, unitPrice, taxPercent, supplierProductCode: lineValue(row, 'supplierProductCode')};
            });
            if (!lines.length) throw new Error('Include at least one invoice item.');
            const existingCount = $('#purchaseLines tr').filter(function () { return !!$(this).find('.line-product-id').val(); }).length;
            if ($('#invoiceApplyHeader').is(':checked')) {
                const supplierName = $('#extractedSupplier').val().trim().toLowerCase();
                const supplier = $('#supplierId option').filter(function () { return this.value && this.text.trim().toLowerCase() === supplierName; });
                if (supplier.length !== 1) {
                    $('#quickSupplierName').val($('#extractedSupplier').val().trim());
                    throw new Error('The supplier name does not match one existing supplier. Use Add Supplier, or select the supplier manually and untick Apply reviewed supplier, date and invoice ID.');
                }
                const invoiceId = $('#extractedInvoiceId').val().trim();
                const date = $('#extractedDate').val();
                if (!invoiceId || !date) throw new Error('Review the invoice ID and date, or untick Apply reviewed supplier, date and invoice ID.');
                if (existingCount && (String($('#supplierId').val()) !== String(supplier.val()) ||
                    ($('#invoiceId').val().trim() && $('#invoiceId').val().trim() !== invoiceId))) {
                    throw new Error('This PO already has items for another supplier or invoice. Start a new PO to import this invoice.');
                }
                if (existingCount && !confirm('This PO already has ' + existingCount + ' items. Add ' + lines.length + ' more? Check for duplicates: saving will increase stock for every added row.')) return;
                // Trigger supplier change before importing so historical mapping cannot overwrite extracted prices/codes.
                if (String($('#supplierId').val()) !== String(supplier.val())) $('#supplierId').val(supplier.val()).trigger('change');
                $('#invoiceId').val(invoiceId); $('#purchaseDate').val(date);
            } else {
                if (!$('#supplierId').val()) throw new Error('Select a supplier before adding the invoice items.');
                if (existingCount && !confirm('Add ' + lines.length + ' invoice items to this PO, which already has ' + existingCount + ' items? Confirm these belong to the same invoice and are not duplicates.')) return;
            }
            $(document).trigger('purchase:import-invoice', [lines]);
            destroyRows(); $('#invoiceReview').prop('hidden', true);
            message(lines.length + ' reviewed items added to the PO. Check totals, then Save & Update Stock.');
        } catch (error) { message(error.message, true); }
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
        if (working || (!$('#invoiceReview').prop('hidden') && rows.length)) {
            event.preventDefault();
            message('Finish adding the reviewed invoice items, or discard the review before saving the PO.', true);
            document.getElementById('invoiceImportCard').scrollIntoView({behavior: 'smooth', block: 'start'});
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
            message('Product created and selected. Review remaining invoice items before adding them to the PO.');
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

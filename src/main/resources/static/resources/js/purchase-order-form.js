$(function () {
        let lineIndex = $('#purchaseLines tr').length;
        const supplierModal = bootstrap.Modal.getOrCreateInstance(document.getElementById('supplierQuickModal'));
        const defaultTaxPercent = Number($('#purchaseOrderPageData').data('default-tax-percent') || 5);
        const dateInput = document.getElementById('purchaseDate');

        function syncPurchaseDate() {
            const iso = PurchaseDate.toIso(dateInput.value);
            $('#purchaseDateIso').val(iso);
            dateInput.setCustomValidity(iso ? '' : 'Enter a valid date as DD-MM-YYYY.');
            return !!iso;
        }

        $('#purchaseDate').on('input', function () {
            const digits = this.value.replace(/\D/g, '').slice(0, 8);
            if (this.value.length >= 2 && !this.value.includes('-') && digits.length === 8) {
                this.value = digits.slice(0, 2) + '-' + digits.slice(2, 4) + '-' + digits.slice(4);
            }
            syncPurchaseDate();
        }).on('change blur', syncPurchaseDate);
        $(document).on('purchase:set-date', function (_, iso) {
            $('#purchaseDate').val(PurchaseDate.fromIso(iso)).trigger('change');
        });
        syncPurchaseDate();

        $('#supplierId').select2({
            theme: 'bootstrap-5',
            width: '100%',
            allowClear: true,
            placeholder: function () {
                return $(this).data('placeholder') || 'Select';
            }
        });

        $('#productSelect').select2({
            theme: 'bootstrap-5',
            width: '100%',
            allowClear: true,
            minimumInputLength: 2,
            placeholder: $('#productSelect').data('placeholder') || 'Search product, brand, or part number',
            ajax: {
                url: '/purchase-orders/product-search',
                dataType: 'json',
                delay: 250,
                data: function (params) {
                    return { keyword: params.term || '' };
                },
                processResults: function (items) {
                    return {
                        results: $.map(items || [], function (item) {
                            return { id: item.id, text: item.text };
                        })
                    };
                }
            }
        });

        function money(value) {
            return (Number(value) || 0).toFixed(2);
        }

        function reindexLines() {
            $('#purchaseLines tr').each(function (index) {
                const productName = $(this).find('.product-label').text();
                $(this).find('.line-quantity').attr('aria-label', 'Quantity for ' + productName);
                $(this).find('.line-unit-price').attr('aria-label', 'Unit price for ' + productName);
                $(this).find('.line-tax-percent').attr('aria-label', 'VAT percent for ' + productName);
                $(this).find('[data-field]').each(function () {
                    $(this).attr('name', 'lines[' + index + '].' + $(this).attr('data-field'));
                });
            });
        }

        function calculateTotals() {
            let subTotalAmount = 0;
            let taxTotalAmount = 0;
            let grandTotal = 0;
            $('#purchaseLines tr').each(function () {
                const quantity = Number($(this).find('.line-quantity').val()) || 0;
                const unitPrice = Number($(this).find('.line-unit-price').val()) || 0;
                const taxPercent = Number($(this).find('.line-tax-percent').val()) || 0;
                const subTotal = quantity * unitPrice;
                const taxAmount = subTotal * taxPercent / 100;
                const lineTotal = subTotal + taxAmount;
                subTotalAmount += subTotal;
                taxTotalAmount += taxAmount;
                grandTotal += lineTotal;
                $(this).find('.line-subtotal').text(money(subTotal));
                $(this).find('.line-tax-amount').text(money(taxAmount));
                $(this).find('.line-total').text(money(lineTotal));
            });
            $('#subTotal').text(money(subTotalAmount));
            $('#taxTotal').text(money(taxTotalAmount));
            $('#grandTotal').text(money(grandTotal));
            const count = $('#purchaseLines tr').length;
            $('#purchaseItemCount').text(count);
            $('#purchaseEmptyState').prop('hidden', count > 0);
            $(document).trigger('purchase:totals-updated', [{total: grandTotal}]);
        }

        function calculateQuickTotal() {
            const quantity = Number($('#quickQuantity').val()) || 0;
            const unitPrice = Number($('#quickUnitPrice').val()) || 0;
            const subTotal = quantity * unitPrice;
            const taxAmount = subTotal * defaultTaxPercent / 100;
            $('#quickLineTotal').text(money(subTotal + taxAmount));
        }

        function loadQuickSupplierProduct(productId) {
            const supplierId = $('#supplierId').val();
            if (!supplierId || !productId) {
                return;
            }
            $.get('/purchase-orders/supplier-product-code', {supplierId: supplierId, productId: productId})
                .done(function (response) {
                    if (response.priceValue && !$('#quickUnitPrice').val()) {
                        $('#quickUnitPrice').val(response.priceValue);
                        calculateQuickTotal();
                    }
                });
        }

        function addLine(product, imported) {
            const rowId = lineIndex++;
            const row = $(
                '<tr>' +
                '<td class="ps-3">' +
                '<input type="hidden" class="line-id" data-field="id">' +
                '<input type="hidden" class="line-product-id" data-field="productId">' +
                '<input type="hidden" class="line-product-text" data-field="productText">' +
                '<div class="fw-semibold product-label purchase-product-label"></div>' +
                '<button type="button" class="btn btn-link btn-sm line-options-toggle d-md-none" aria-expanded="false">Details</button>' +
                '<input type="text" class="form-control form-control-sm supplier-code purchase-supplier-code mt-1" data-field="supplierProductCode" maxlength="100" aria-label="Supplier product code" placeholder="Supplier product code">' +
                '</td>' +
                '<td data-label="Qty"><input type="number" class="form-control form-control-sm line-quantity" data-field="quantity" min="1" step="1" value="1"></td>' +
                '<td data-label="Unit Price"><input type="number" class="form-control form-control-sm line-unit-price" data-field="unitPrice" min="0.01" step="0.01" placeholder="0.00"></td>' +
                '<td class="text-end fw-semibold line-subtotal" data-label="Gross">0.00</td>' +
                '<td class="purchase-line-vat" data-label="VAT %"><input type="number" class="form-control form-control-sm line-tax-percent" data-field="taxPercent" min="0" max="100" step="0.01" value="0"></td>' +
                '<td class="text-end fw-semibold line-tax-amount" data-label="VAT">0.00</td>' +
                '<td class="text-end fw-semibold line-total" data-label="Total incl. VAT">0.00</td>' +
                '<td class="text-center pe-3" data-label="Action"><button type="button" class="btn btn-sm btn-outline-danger remove-line" aria-label="Remove product" title="Remove product"><i class="fas fa-trash-alt" aria-hidden="true"></i></button></td>' +
                '</tr>'
            ).attr('data-row-id', rowId);
            row.find('.line-product-id').val(product.id);
            row.find('.line-product-text').val(product.text);
            row.find('.product-label').text(product.text);
            row.find('.line-quantity').val(imported ? imported.quantity : ($('#quickQuantity').val() || '1'));
            row.find('.line-unit-price').val(imported ? imported.unitPrice : ($('#quickUnitPrice').val() || ''));
            row.find('.line-tax-percent').val(imported ? imported.taxPercent : defaultTaxPercent.toFixed(2));
            if (imported) row.find('.supplier-code').val(imported.supplierProductCode || '');
            $('#purchaseLines').prepend(row);
            $('.purchase-lines-wrapper').scrollTop(0);
            reindexLines();
            if (!imported) loadSupplierProduct(row);
            calculateTotals();
            return row;
        }

        $(document).on('purchase:import-invoice', function (_, lines) {
            lines.slice().reverse().forEach(function (line) { addLine(line.product, line); });
        });

        $(document).on('purchase:load-extracted', function (_, batch) {
            batch.rows = batch.lines.slice().reverse().map(function (line) {
                const text = [line.name || 'Unmatched invoice item', line.brand, line.partNumber].filter(Boolean).join(' | ');
                return addLine({id: '', text: text}, line).attr('data-invoice-source', 'ai').data('extracted-line', line);
            }).reverse();
        });
        $(document).on('purchase:refresh-lines', function () { reindexLines(); calculateTotals(); });

        function loadSupplierProduct(row, supplierChanged) {
            const supplierId = $('#supplierId').val();
            const productId = row.find('.line-product-id').val();
            if (supplierChanged) row.find('.supplier-code').val('');
            const requestedCode = row.find('.supplier-code').val();
            if (!supplierId || !productId) {
                return;
            }
            $.get('/purchase-orders/supplier-product-code', {supplierId: supplierId, productId: productId})
                .done(function (response) {
                    if ($('#supplierId').val() !== supplierId || !row.closest(document.documentElement).length) return;
                    if (response.supplierProductCode && row.find('.supplier-code').val() === requestedCode && !requestedCode) {
                        row.find('.supplier-code').val(response.supplierProductCode);
                    }
                    if (response.priceValue && !row.find('.line-unit-price').val()) {
                        row.find('.line-unit-price').val(response.priceValue);
                    }
                    calculateTotals();
                });
        }

        $('#supplierId').on('change', function () {
            $('#purchaseLines tr').each(function () {
                if (!$(this).is('[data-invoice-source]')) loadSupplierProduct($(this), true);
            });
            if ($('#productSelect').val()) {
                $('#quickUnitPrice').val('');
                calculateQuickTotal();
                loadQuickSupplierProduct($('#productSelect').val());
            }
        });

        $('#purchaseLines').on('input', '.line-quantity, .line-unit-price, .line-tax-percent', calculateTotals);

        $('#purchaseLines').on('click', '.line-options-toggle', function () {
            const expanded = $(this).closest('tr').toggleClass('is-expanded').hasClass('is-expanded');
            $(this).attr('aria-expanded', String(expanded)).text(expanded ? 'Hide details' : 'Details');
        });

        $('#purchaseLines').on('click', '.remove-line', function () {
            $(this).closest('tr').remove();
            reindexLines();
            calculateTotals();
        });

        $('#purchaseLines').on('keydown', '.line-unit-price', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                $('#productSelect').select2('open');
            }
        });

        $('#addSelectedProductButton').on('click', function () {
            const selectedProductId = $('#productSelect').val();
            if (!selectedProductId) {
                alert('Please select a product from search results.');
                $('#productSelect').select2('open');
                return;
            }
            const quantity = Number($('#quickQuantity').val()) || 0;
            if (quantity <= 0 || quantity % 1 !== 0) {
                alert('Quantity must be a whole number greater than zero.');
                $('#quickQuantity').trigger('focus').select();
                return;
            }
            const unitPrice = Number($('#quickUnitPrice').val()) || 0;
            if (unitPrice <= 0) {
                alert('Unit price must be greater than zero.');
                $('#quickUnitPrice').trigger('focus').select();
                return;
            }
            const selectedOption = $('#productSelect').select2('data')[0];
            addLine({
                id: selectedProductId,
                text: selectedOption ? selectedOption.text : 'Selected product'
            });
            $('#productSelect').val('').trigger('change');
            $('#quickQuantity').val('1');
            $('#quickUnitPrice').val('');
            calculateQuickTotal();
            $('#productSelect').select2('open');
        });

        $('#productSelect').on('select2:select', function () {
            $('#quickUnitPrice').val('');
            calculateQuickTotal();
            loadQuickSupplierProduct($('#productSelect').val());
            $('#quickQuantity').trigger('focus').select();
        });

        $('#quickQuantity, #quickUnitPrice').on('input', calculateQuickTotal);

        $('#quickQuantity').on('keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                $('#quickUnitPrice').trigger('focus').select();
            }
        });

        $('#quickUnitPrice').on('keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                $('#addSelectedProductButton').trigger('click');
            }
        });

        $('#purchaseForm').on('input change', 'input:not([type="hidden"]), select, textarea', function () {
            $('#purchaseFormError').addClass('d-none').empty();
            $('.purchase-server-error').filter(function () { return $(this).text().trim() === 'Please check purchase order details.'; }).addClass('d-none');
            const valid = typeof this.checkValidity === 'function' && this.checkValidity();
            $(this).removeClass('is-invalid').toggleClass('is-valid', valid && !!$(this).val())
                .attr('aria-invalid', String(!valid));
            if (this.id === 'storeId' && this.value) {
                $('.purchase-server-error').filter(function () { return /\b(warehouse|store)\b/i.test($(this).text()); }).addClass('d-none');
                const notice = $('#invoiceImportMessage');
                if (notice.hasClass('alert-danger') && /\b(warehouse|store)\b/i.test(notice.text())) {
                    notice.removeClass('alert-danger').addClass('alert-light').text('Warehouse selected. Review your purchase items before saving.');
                }
            }
        });

        $('#purchaseForm').on('submit', function (event) {
            if (!syncPurchaseDate()) {
                event.preventDefault();
                $('#purchaseFormError').text('Enter a valid invoice date as DD-MM-YYYY.').removeClass('d-none');
                return;
            }
            let error = null;
            $('#purchaseLines tr').each(function (index) {
                const row = $(this);
                const quantity = Number(row.find('.line-quantity').val());
                const price = Number(row.find('.line-unit-price').val());
                const tax = Number(row.find('.line-tax-percent').val());
                if (!row.find('.line-product-id').val()) error = error || 'Choose or create a product for item ' + (index + 1) + '.';
                else if (!Number.isSafeInteger(quantity) || quantity <= 0) error = error || 'Enter a whole quantity greater than zero for item ' + (index + 1) + '.';
                else if (!Number.isFinite(price) || price <= 0) error = error || 'Enter a valid unit price for item ' + (index + 1) + '.';
                else if (row.find('.line-tax-percent').val() === '' || !Number.isFinite(tax) || tax < 0 || tax > 100) error = error || 'Enter VAT between 0 and 100 for item ' + (index + 1) + '.';
                else if ([price, tax].some(value => Math.abs(value * 100 - Math.round(value * 100)) > 0.000001)) error = error || 'Use at most two decimals for unit price and VAT for item ' + (index + 1) + '.';
            });
            if (error) {
                event.preventDefault();
                $('#purchaseFormError').text(error).removeClass('d-none');
                return;
            }
            $('#purchaseFormError').addClass('d-none').empty();
            let validLines = 0;
            $('#purchaseLines tr').each(function () {
                if ($(this).find('.line-product-id').val()) {
                    validLines++;
                }
            });
            if (validLines === 0) {
                event.preventDefault();
                alert('Add at least one product.');
                $('#productSelect').select2('open');
            }
        });

        $('#supplierQuickForm').on('submit', function (event) {
            event.preventDefault();
            $('#supplierQuickError').addClass('d-none').empty();
            $('#saveQuickSupplierButton').prop('disabled', true).text('Adding...');
            $.ajax({
                url: '/api/v1/suppliers',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    name: ($('#quickSupplierName').val() || '').trim(),
                    contactPerson: ($('#quickSupplierContactPerson').val() || '').trim() || null,
                    phone: ($('#quickSupplierPhone').val() || '').trim() || null,
                    email: ($('#quickSupplierEmail').val() || '').trim() || null,
                    trnNumber: ($('#quickSupplierTrn').val() || '').trim() || null,
                    address: ($('#quickSupplierAddress').val() || '').trim() || null,
                    active: $('#quickSupplierActive').is(':checked')
                })
            }).done(function (supplier) {
                const option = $('<option>', {value: supplier.id, text: supplier.name});
                $('#supplierId').append(option).val(supplier.id).trigger('change');
                $(document).trigger('purchase:supplier-created', [supplier]);
                $('#quickSupplierName, #quickSupplierContactPerson, #quickSupplierPhone, #quickSupplierEmail, #quickSupplierTrn, #quickSupplierAddress').val('');
                $('#quickSupplierActive').prop('checked', true);
                supplierModal.hide();
                $('.modal-backdrop').remove();
                $('body').removeClass('modal-open').css('padding-right', '');
                $('#invoiceId').trigger('focus');
            }).fail(function (xhr) {
                const message = xhr.responseJSON && (xhr.responseJSON.error || xhr.responseJSON.message)
                    ? (xhr.responseJSON.error || xhr.responseJSON.message)
                    : 'Could not add supplier.';
                $('#supplierQuickError').text(message).removeClass('d-none');
            }).always(function () {
                $('#saveQuickSupplierButton').prop('disabled', false).text('Add Supplier');
            });
        });
        $('#supplierQuickModal').on('hidden.bs.modal', function () {
            $('.modal-backdrop').remove();
            $('body').removeClass('modal-open').css('padding-right', '');
        });

        reindexLines();
        calculateTotals();
        calculateQuickTotal();
    });

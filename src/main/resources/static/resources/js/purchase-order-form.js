$(function () {
        let lineIndex = $('#purchaseLines tr').length;
        const supplierModal = bootstrap.Modal.getOrCreateInstance(document.getElementById('supplierQuickModal'));
        const defaultTaxPercent = Number($('#purchaseOrderPageData').data('default-tax-percent') || 5);

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

        function addLine(product) {
            const rowId = lineIndex++;
            const row = $(
                '<tr>' +
                '<td class="ps-3">' +
                '<input type="hidden" class="line-id" data-field="id">' +
                '<input type="hidden" class="line-product-id" data-field="productId">' +
                '<input type="hidden" class="line-product-text" data-field="productText">' +
                '<div class="fw-semibold product-label"></div>' +
                '<div class="small text-muted">Selected product</div>' +
                '</td>' +
                '<td><input type="text" class="form-control supplier-code" data-field="supplierProductCode" maxlength="100" placeholder="Supplier code"></td>' +
                '<td><input type="number" class="form-control line-quantity" data-field="quantity" min="1" step="1" value="1"></td>' +
                '<td><input type="number" class="form-control line-unit-price" data-field="unitPrice" min="0.01" step="0.01" placeholder="0.00"></td>' +
                '<td class="text-end fw-semibold line-subtotal">0.00</td>' +
                '<td><input type="number" class="form-control line-tax-percent" data-field="taxPercent" min="0" step="0.01" value="0"></td>' +
                '<td class="text-end fw-semibold line-tax-amount">0.00</td>' +
                '<td class="text-end fw-semibold line-total">0.00</td>' +
                '<td class="text-end pe-3"><button type="button" class="btn btn-sm btn-outline-danger remove-line">Remove</button></td>' +
                '</tr>'
            ).attr('data-row-id', rowId);
            row.find('.line-product-id').val(product.id);
            row.find('.line-product-text').val(product.text);
            row.find('.product-label').text(product.text);
            row.find('.line-quantity').val($('#quickQuantity').val() || '1');
            row.find('.line-unit-price').val($('#quickUnitPrice').val() || '');
            row.find('.line-tax-percent').val(defaultTaxPercent.toFixed(2));
            $('#purchaseLines').prepend(row);
            reindexLines();
            loadSupplierProduct(row);
            calculateTotals();
        }

        function loadSupplierProduct(row) {
            const supplierId = $('#supplierId').val();
            const productId = row.find('.line-product-id').val();
            if (!supplierId || !productId) {
                return;
            }
            $.get('/purchase-orders/supplier-product-code', {supplierId: supplierId, productId: productId})
                .done(function (response) {
                    if (response.supplierProductCode) {
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
                loadSupplierProduct($(this));
            });
            if ($('#productSelect').val()) {
                $('#quickUnitPrice').val('');
                calculateQuickTotal();
                loadQuickSupplierProduct($('#productSelect').val());
            }
        });

        $('#purchaseLines').on('input', '.line-quantity, .line-unit-price, .line-tax-percent', calculateTotals);

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

        $('#purchaseForm').on('submit', function (event) {
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

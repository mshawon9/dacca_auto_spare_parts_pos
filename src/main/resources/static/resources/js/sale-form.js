$(function () {
        const saleFormPageData = $('#saleFormPageData');
        let draftId = saleFormPageData.data('draft-id') || null;
        let selectedProduct = null;
        let currentDraft = null;
        const defaultStoreId = saleFormPageData.data('default-store-id') || null;
        const saleReviewModal = bootstrap.Modal.getOrCreateInstance(document.getElementById('saleReviewModal'));
        const flashSuccess = saleFormPageData.data('success-message') || null;
        const flashError = saleFormPageData.data('error-message') || null;
        const completedInvoiceNo = saleFormPageData.data('completed-invoice-no') || null;
        const completedInvoicePdfUrl = saleFormPageData.data('completed-invoice-pdf-url') || null;
        let hydratingDraft = false;

        function money(value) {
            return (Number(value) || 0).toFixed(2);
        }

        function qty(value) {
            return (Number(value) || 0).toString();
        }

        function showToast(message, type, title) {
            if (!message) {
                return;
            }
            const toastType = type || 'info';
            const toastTitle = title || (toastType === 'danger' ? 'Error' : (toastType === 'success' ? 'Success' : 'Notice'));
            const iconClass = toastType === 'danger'
                ? 'bi-exclamation-triangle-fill'
                : (toastType === 'success' ? 'bi-check-circle-fill' : 'bi-info-circle-fill');
            const $toast = $('<div class="toast" role="alert" aria-live="assertive" aria-atomic="true" data-bs-autohide="true" data-bs-delay="5000"></div>')
                .addClass('toast-' + toastType);
            const $header = $('<div class="toast-header"></div>');
            $('<i class="bi me-2"></i>').addClass(iconClass).appendTo($header);
            $('<strong class="me-auto"></strong>').text(toastTitle).appendTo($header);
            $('<small>now</small>').appendTo($header);
            $('<button type="button" class="btn-close ms-2 mb-1" data-bs-dismiss="toast" aria-label="Close"></button>').appendTo($header);
            const $body = $('<div class="toast-body"></div>').text(message);
            $toast.append($header).append($body).appendTo('#saleToastContainer');
            const toast = bootstrap.Toast.getOrCreateInstance($toast[0], {
                autohide: true,
                delay: 5000
            });
            $toast.on('hidden.bs.toast', function () {
                $toast.remove();
            });
            toast.show();
        }

        function showError(message) {
            $('#saleError').addClass('d-none').empty();
            showToast(message || 'Something went wrong.', 'danger', 'Sale Notification');
        }

        function showSuccess(message) {
            showToast(message, 'success', 'Sale Notification');
        }

        function clearError() {
            $('#saleError').addClass('d-none').empty();
        }

        function closeUnfinishedDraftModal() {
            const modalElement = document.getElementById('unfinishedSalesModal');
            const modal = bootstrap.Modal.getInstance(modalElement) || bootstrap.Modal.getOrCreateInstance(modalElement);
            modal.hide();
            setTimeout(function () {
                $('.modal-backdrop').remove();
                $('body').removeClass('modal-open').css({
                    overflow: '',
                    paddingRight: ''
                });
            }, 200);
        }

        function headerPayload() {
            return {
                customerId: $('#customerId').val() || null,
                storeId: $('#storeId').val() || null,
                saleDate: $('#saleDate').val(),
                saleType: $('#saleType').val(),
                vatMode: $('#vatMode').val(),
                vatPercent: $('#vatPercent').val(),
                paymentMethod: $('#paymentMethod').val(),
                paidAmount: $('#paidAmount').val() || 0,
                note: null
            };
        }

        function ensureDraft(callback) {
            if (draftId) {
                callback();
                return;
            }
            $.post('/sales/drafts')
                .done(function (draft) {
                    draftId = draft.id;
                    $('#draftIdText').text('#' + draftId);
                    $('#completeSaleForm').attr('action', '/sales/drafts/' + draftId + '/complete');
                    currentDraft = draft;
                    callback();
                })
                .fail(function (xhr) {
                    showError(xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : 'Could not start sale draft.');
                });
        }

        function saveHeader(callback) {
            ensureDraft(function () {
                $.ajax({
                    url: '/sales/drafts/' + draftId + '/header',
                    method: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify(headerPayload())
                }).done(function (draft) {
                    currentDraft = draft;
                    renderDraft(draft);
                    if (callback) {
                        callback();
                    }
                }).fail(function (xhr) {
                    showError(xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : 'Could not save sale details.');
                });
            });
        }

        $('#customerId').select2({
            theme: 'bootstrap-5',
            width: '100%',
            allowClear: true,
            minimumInputLength: 2,
            placeholder: $('#customerId').data('placeholder') || 'Search customer',
            ajax: {
                url: '/sales/customer-search',
                dataType: 'json',
                delay: 250,
                data: function (params) {
                    return {keyword: params.term || ''};
                },
                processResults: function (items) {
                    return {results: $.map(items || [], function (item) { return {id: item.id, text: item.text}; })};
                }
            }
        });

        $('#productSelect').select2({
            theme: 'bootstrap-5',
            width: '100%',
            allowClear: true,
            minimumInputLength: 2,
            placeholder: $('#productSelect').data('placeholder') || 'Search product, brand, or part number',
            ajax: {
                url: '/sales/product-search',
                dataType: 'json',
                delay: 250,
                data: function (params) {
                    const data = {
                        keyword: params.term || ''
                    };
                    const storeId = $('#storeId').val();
                    if (storeId) {
                        data.storeId = storeId;
                    }
                    const customerId = $('#customerId').val();
                    if (customerId) {
                        data.customerId = customerId;
                    }
                    return data;
                },
                processResults: function (items) {
                    return {
                        results: $.map(items || [], function (item) {
                            return {
                                id: item.id,
                                text: item.text,
                                stockQuantity: item.stockQuantity,
                                lastCostPrice: item.lastCostPrice,
                                sellingPrice: item.sellingPrice,
                                customerPrice: item.customerPrice
                            };
                        })
                    };
                }
            }
        });

        $('#productSelect').on('select2:opening', function (event) {
            if (!$('#storeId').val()) {
                event.preventDefault();
                showError('Please select warehouse before searching product.');
            }
        });

        function applyProductInfo(item) {
            selectedProduct = item;
            $('#availableStock').text(qty(item.stockQuantity));
            $('#lastCostPrice').text(item.lastCostPrice == null ? '-' : money(item.lastCostPrice));
            const price = item.customerPrice || item.sellingPrice || '';
            $('#quickUnitPrice').val(price);
            $('#priceSource').text(item.customerPrice ? 'Customer last price' : (item.sellingPrice ? 'Warehouse selling price' : 'Manual'));
        }

        $('#productSelect').on('select2:select', function (event) {
            applyProductInfo(event.params.data);
            $('#quickQuantity').trigger('focus').select();
        });

        $('#customerId, #storeId').on('change', function () {
            if (hydratingDraft) {
                return;
            }
            $('#productSelect').val('').trigger('change');
            selectedProduct = null;
            $('#availableStock').text('0');
            $('#lastCostPrice').text('-');
            $('#priceSource').text('-');
            $('#quickUnitPrice').val('');
            if (draftId) {
                saveHeader();
            }
        });

        $('#saleDate, #saleType, #vatMode, #vatPercent, #paymentMethod, #paidAmount').on('change blur', function () {
            if (hydratingDraft) {
                return;
            }
            if (draftId) {
                saveHeader();
            }
        });

        function fillCustomer(customerId, customerName) {
            const $customer = $('#customerId');
            if (!customerId) {
                $customer.val(null).trigger('change.select2');
                return;
            }
            const value = String(customerId);
            if ($customer.find("option[value='" + value.replace(/'/g, "\\'") + "']").length === 0) {
                $customer.append(new Option(customerName || 'Customer #' + value, value, true, true));
            }
            $customer.val(value).trigger('change.select2');
        }

        function applyDraftFields(draft) {
            if (!draft) {
                return;
            }
            hydratingDraft = true;
            $('#draftIdText').text('#' + draft.id);
            $('#completeSaleForm').attr('action', '/sales/drafts/' + draft.id + '/complete');
            fillCustomer(draft.customerId, draft.customerName);
            $('#storeId').val(draft.storeId);
            $('#saleDate').val(draft.saleDate);
            $('#saleType').val(draft.saleType);
            $('#vatMode').val(draft.vatMode);
            $('#vatPercent').val(draft.vatPercent);
            $('#paymentMethod').val(draft.paymentMethod === 'CREDIT' ? 'CASH' : draft.paymentMethod);
            $('#paidAmount').val(draft.paidAmount);
            hydratingDraft = false;
        }

        function renderDraft(draft) {
            currentDraft = draft;
            $('#subTotal').text(money(draft ? draft.subTotal : 0));
            $('#vatTotal').text(money(draft ? draft.vatAmount : 0));
            $('#grandTotal').text(money(draft ? draft.total : 0));
            $('#lineCount').text(draft && draft.lines ? draft.lines.length : 0);
            const hasLines = draft && draft.lines && draft.lines.length > 0;
            $('#storeId').prop('disabled', hasLines);
            $('#warehouseFixedText').toggleClass('d-none', !hasLines);
            if (draft) {
                applyDraftFields(draft);
                const isCredit = draft.saleType !== 'REGULAR';
                $('#paymentMethod, #paidAmount').prop('disabled', isCredit);
            }
            $('#saleLines').empty();
            $.each((draft && draft.lines) || [], function (_, line) {
                $('<tr>' +
                    '<td class="ps-3"><div class="fw-semibold"></div></td>' +
                    '<td class="text-end"></td>' +
                    '<td class="text-end"></td>' +
                    '<td class="text-end"></td>' +
                    '<td class="text-end fw-semibold"></td>' +
                    '<td class="text-end pe-3"><button type="button" class="btn btn-sm btn-outline-danger remove-line">Remove</button></td>' +
                    '</tr>')
                    .attr('data-line-id', line.id)
                    .find('td:eq(0) .fw-semibold').text(line.productText).end()
                    .find('td:eq(1)').text(qty(line.quantity)).end()
                    .find('td:eq(2)').text(money(line.unitPrice)).end()
                    .find('td:eq(3)').text(money(line.vatAmount)).end()
                    .find('td:eq(4)').text(money(line.lineTotal)).end()
                    .appendTo('#saleLines');
            });
        }

        $('#addSelectedProductButton').on('click', function () {
            clearError();
            const productId = $('#productSelect').val();
            const quantity = Number($('#quickQuantity').val()) || 0;
            const unitPrice = Number($('#quickUnitPrice').val()) || 0;
            if (!productId) {
                showError('Please select a product.');
                $('#productSelect').select2('open');
                return;
            }
            if (quantity <= 0 || quantity % 1 !== 0) {
                showError('Quantity must be a whole number greater than zero.');
                $('#quickQuantity').trigger('focus').select();
                return;
            }
            if (unitPrice <= 0) {
                showError('Unit price must be greater than zero.');
                $('#quickUnitPrice').trigger('focus').select();
                return;
            }
            saveHeader(function () {
                $.ajax({
                    url: '/sales/drafts/' + draftId + '/lines',
                    method: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify({productId: productId, quantity: quantity, unitPrice: unitPrice})
                }).done(function (draft) {
                    clearError();
                    renderDraft(draft);
                    showSuccess('Product added to sale.');
                    $('#productSelect').val('').trigger('change').select2('open');
                    $('#quickQuantity').val('1');
                    $('#quickUnitPrice').val('');
                    $('#availableStock').text('0');
                    $('#lastCostPrice').text('-');
                    $('#priceSource').text('-');
                    selectedProduct = null;
                }).fail(function (xhr) {
                    showError(xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : 'Could not add product.');
                });
            });
        });

        $('#saleLines').on('click', '.remove-line', function () {
            const lineId = $(this).closest('tr').attr('data-line-id');
            $.ajax({
                url: '/sales/drafts/' + draftId + '/lines/' + lineId,
                method: 'DELETE'
            }).done(function (draft) {
                renderDraft(draft);
                showSuccess('Product removed from sale.');
            }).fail(function (xhr) {
                showError(xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : 'Could not remove line.');
            });
        });

        $('#unfinishedDraftList').on('click', '.js-delete-draft', function (event) {
            event.preventDefault();
            event.stopPropagation();
            const id = $(this).data('id');
            const $row = $(this).closest('[data-draft-id]');
            if (!id || !confirm('Delete this unfinished sale draft?')) {
                return;
            }
            $.ajax({
                url: '/sales/drafts/' + id,
                method: 'DELETE'
            }).done(function () {
                $row.remove();
                const remaining = $('#unfinishedDraftList [data-draft-id]').length;
                $('.js-open-drafts-count').text(remaining);
                showSuccess('Unfinished sale draft deleted.');
                if (draftId && Number(draftId) === Number(id)) {
                    window.location.href = '/sales/create';
                    return;
                }
                closeUnfinishedDraftModal();
            }).fail(function (xhr) {
                showError(xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : 'Could not delete draft.');
            });
        });

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

        function validateBeforeReview() {
            if (!draftId || !currentDraft || !currentDraft.lines || currentDraft.lines.length === 0) {
                showError('Add at least one product before sale.');
                return false;
            }
            if (!$('#customerId').val() && $('#saleType').val() !== 'REGULAR') {
                showError('Customer is required for credit sale.');
                return false;
            }
            return true;
        }

        $('#reviewSaleButton').on('click', function () {
            clearError();
            if (!validateBeforeReview()) {
                return;
            }
            saveHeader(function () {
                $('#reviewHeader').text(
                    ($('#customerId option:selected').text() || 'Customer')
                    + ' | ' + $('#saleType option:selected').text()
                    + ' | ' + $('#saleDate').val()
                );
                $('#reviewLines').empty();
                $.each(currentDraft.lines || [], function (_, line) {
                    $('<tr><td></td><td class="text-end"></td><td class="text-end"></td><td class="text-end"></td><td class="text-end fw-semibold"></td></tr>')
                        .find('td:eq(0)').text(line.productText).end()
                        .find('td:eq(1)').text(qty(line.quantity)).end()
                        .find('td:eq(2)').text(money(line.unitPrice)).end()
                        .find('td:eq(3)').text(money(line.vatAmount)).end()
                        .find('td:eq(4)').text(money(line.lineTotal)).end()
                        .appendTo('#reviewLines');
                });
                $('#reviewSubTotal').text(money(currentDraft.subTotal));
                $('#reviewVatTotal').text(money(currentDraft.vatAmount));
                $('#reviewPaidTotal').text(money(currentDraft.paidAmount));
                $('#reviewTotal').text(money(currentDraft.total));
                saleReviewModal.show();
            });
        });

        $('#confirmSaleButton').on('click', function () {
            $('#completeSaleForm').trigger('submit');
        });

        $('#completeSaleForm').on('submit', function (event) {
            if (!validateBeforeReview()) {
                event.preventDefault();
                $('#confirmSaleButton').prop('disabled', false).text('Confirm Sale');
                return;
            }
            $('#confirmSaleButton').prop('disabled', true).text('Completing...');
        });

        if (!$('#storeId').val() && defaultStoreId) {
            $('#storeId').val(defaultStoreId);
        }
        if (flashSuccess) {
            showSuccess(flashSuccess);
        }
        if (completedInvoicePdfUrl) {
            $('#openInvoicePdfButton').attr('href', completedInvoicePdfUrl);
            $('#invoicePdfPromptSubtitle').text('Invoice ' + (completedInvoiceNo || '') + ' completed successfully.');
            bootstrap.Modal.getOrCreateInstance(document.getElementById('invoicePdfPromptModal')).show();
        }
        if (flashError) {
            showError(flashError);
        }
        if (draftId) {
            $.get('/sales/drafts/' + draftId)
                .done(function (draft) {
                    renderDraft(draft);
                })
                .fail(function (xhr) {
                    showError(xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : 'Could not load unfinished sale.');
                    renderDraft(currentDraft);
                });
        } else {
            renderDraft(currentDraft);
        }
    });
$(function () {
        function money(value) {
            return (Number(value) || 0).toFixed(2);
        }

        function quantity(value) {
            const number = Number(value) || 0;
            return Number.isInteger(number) ? String(number) : String(number);
        }

        function loadPo(id, onSuccess, onError) {
            $.get('/purchase-orders/' + id + '/detail-json')
                .done(onSuccess)
                .fail(function () {
                    if (onError) {
                        onError();
                    }
                });
        }

        $('.js-po-details').on('click', function () {
            const id = $(this).attr('data-id');
            $('#poDetailsError').addClass('d-none').empty();
            $('#poDetailsHeader').text('Loading...');
            $('#poDetailsLines').empty();
            loadPo(id, function (po) {
                $('#poDetailsHeader').text('Invoice ' + po.invoiceId + ' | ' + po.supplierName + ' | ' + po.storeName + ' | Total ' + money(po.total));
                $.each(po.lines || [], function (_, line) {
                    $('<tr>' +
                        '<td><div class="fw-semibold"></div><div class="small text-muted"></div></td>' +
                        '<td></td>' +
                        '<td class="text-end"></td>' +
                        '<td class="text-end"></td>' +
                        '<td class="text-end"></td>' +
                        '<td class="text-end"></td>' +
                        '<td class="text-end fw-semibold"></td>' +
                        '</tr>')
                        .find('td:eq(0) .fw-semibold').text(line.productName).end()
                        .find('td:eq(0) .small').text('Brand: ' + line.brandName + ' | Part: ' + line.partNumber).end()
                        .find('td:eq(1)').text(line.supplierProductCode || '-').end()
                        .find('td:eq(2)').text(quantity(line.quantity)).end()
                        .find('td:eq(3)').text(quantity(line.returnedQuantity)).end()
                        .find('td:eq(4)').text(money(line.unitPrice)).end()
                        .find('td:eq(5)').text(money(line.taxAmount)).end()
                        .find('td:eq(6)').text(money(line.lineTotal)).end()
                        .appendTo('#poDetailsLines');
                });
            }, function () {
                $('#poDetailsHeader').text('Could not load purchase order.');
                $('#poDetailsError').text('Could not load purchase order details.').removeClass('d-none');
            });
        });

        $('.js-po-return').on('click', function () {
            const id = $(this).attr('data-id');
            $('#poReturnError').addClass('d-none').empty();
            $('#poReturnHeader').text('Loading...');
            $('#returnLineId').empty();
            $('#returnQuantity').val('');
            $('#poReturnForm').attr('action', '/purchase-orders/' + id + '/return');
            $('#returnSubmitButton').prop('disabled', true);
            loadPo(id, function (po) {
                $('#poReturnHeader').text('Invoice ' + po.invoiceId + ' | ' + po.supplierName + ' | Warehouse: ' + po.storeName);
                const returnableLines = (po.lines || []).filter(function (line) {
                    return Number(line.returnableQuantity) > 0;
                });
                if (!returnableLines.length) {
                    $('#returnLineId').append($('<option>', {value: '', text: 'No returnable item left'}));
                    $('#poReturnError').text('All items in this PO are already fully returned.').removeClass('d-none');
                    return;
                }
                $('#returnLineId').append($('<option>', {value: '', text: 'Select item'}));
                $.each(returnableLines, function (_, line) {
                    $('<option>', {
                        value: line.id,
                        text: line.productName + ' | Brand: ' + line.brandName + ' | Part: ' + line.partNumber + ' | Returnable: ' + quantity(line.returnableQuantity)
                    }).attr('data-max', line.returnableQuantity).appendTo('#returnLineId');
                });
                $('#returnSubmitButton').prop('disabled', false);
            }, function () {
                $('#poReturnHeader').text('Could not load purchase order.');
                $('#poReturnError').text('Could not load purchase order details.').removeClass('d-none');
            });
        });

        $('#returnLineId').on('change', function () {
            const max = Number($(this).find('option:selected').attr('data-max')) || 0;
            $('#returnQuantity').attr('max', max || '').val(max ? 1 : '');
        });

        $('#poReturnForm').on('submit', function (event) {
            const max = Number($('#returnLineId option:selected').attr('data-max')) || 0;
            const quantityValue = Number($('#returnQuantity').val()) || 0;
            if (!$('#returnLineId').val() || quantityValue <= 0 || quantityValue % 1 !== 0 || quantityValue > max) {
                event.preventDefault();
                $('#poReturnError')
                    .text('Return quantity must be a whole number and cannot be more than returnable quantity.')
                    .removeClass('d-none');
            }
        });
    });
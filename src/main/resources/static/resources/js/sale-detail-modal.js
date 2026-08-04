$(function () {
        function money(value) {
            return (Number(value) || 0).toFixed(2);
        }

        function qty(value) {
            return (Number(value) || 0).toString();
        }

        $('.js-sale-details').on('click', function () {
            const id = $(this).data('id');
            $('#saleDetailsError').addClass('d-none').empty();
            $('#saleDetailsHeader').text('Loading...');
            $('#saleDetailsLines').empty();

            $.get('/sales/' + id + '/detail-json')
                .done(function (sale) {
                    $('#saleDetailsHeader').text(sale.invoiceNo + ' | ' + sale.saleDate + ' | VAT ' + sale.vatMode + ' ' + money(sale.vatPercent) + '%');
                    $('#saleDetailCustomer').text(sale.customerName || 'Walk-in customer');
                    $('#saleDetailStore').text(sale.storeName || '-');
                    $('#saleDetailType').text(sale.saleType || '-');
                    $('#saleDetailPayment').text(sale.paymentMethod || '-');
                    $('#saleDetailDueDate').text(sale.dueDate || '-');
                    $('#saleDetailSubTotal').text(money(sale.subTotal));
                    $('#saleDetailVat').text(money(sale.vatAmount));
                    $('#saleDetailTotal').text(money(sale.total));
                    $('#saleDetailPaid').text(money(sale.paidAmount));
                    $('#saleDetailBalance').text(money(sale.balanceDue));

                    $.each(sale.lines || [], function (_, line) {
                        $('<tr><td></td><td></td><td></td><td class="text-end"></td><td class="text-end"></td><td class="text-end fw-semibold"></td></tr>')
                            .find('td:eq(0)').text([line.categoryName, line.productName].filter(Boolean).join(' | ')).end()
                            .find('td:eq(1)').text(line.brandName || '-').end()
                            .find('td:eq(2)').text(line.partNumber || '-').end()
                            .find('td:eq(3)').text(qty(line.quantity)).end()
                            .find('td:eq(4)').text(money(line.unitPrice)).end()
                            .find('td:eq(5)').text(money(line.lineTotal)).end()
                            .appendTo('#saleDetailsLines');
                    });
                })
                .fail(function (xhr) {
                    $('#saleDetailsHeader').text('Could not load sale.');
                    $('#saleDetailsError')
                        .text(xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : 'Could not load sale details.')
                        .removeClass('d-none');
                });
        });
    });

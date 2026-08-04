$(function () {
        function money(value) {
            const number = Number(value || 0);
            return number.toFixed(2);
        }

        function syncChequeFields() {
            $('.js-cheque-fields').toggleClass('d-none', $('#paymentMethod').val() !== 'CHEQUE');
        }

        function syncMonthlyChequeFields() {
            $('.js-monthly-cheque-fields').toggleClass('d-none', $('#monthlyPaymentMethod').val() !== 'CHEQUE');
        }

        function escapeHtml(value) {
            return $('<div>').text(value || '').html();
        }

        $('.js-collect').on('click', function () {
            const $btn = $(this);
            $('#collectForm').attr('action', '/credit-collections/' + $btn.data('id') + '/collect');
            $('#collectSubtitle').text($btn.data('invoice') + ' | ' + $btn.data('customer') + ' | Due: ' + money($btn.data('due')));
            $('#amount').val(money($btn.data('due')));
            $('#collectionDueDate').val($btn.data('due-date') || '');
            $('#receiveDate').val($('#creditCollectionPageData').data('today') || '');
            $('#paymentMethod').val('CASH');
            $('#chequeNumber, #chequeDate, #note').val('');
            syncChequeFields();
        });

        $('.js-due-date').on('click', function () {
            const $btn = $(this);
            $('#dueDateForm').attr('action', '/credit-collections/' + $btn.data('id') + '/due-date');
            $('#dueDateTitle').text('Update Due Date - ' + $btn.data('invoice'));
            $('#dueDate').val($btn.data('due-date') || '');
        });

        $('.js-monthly-collect').on('click', function () {
            const $btn = $(this);
            $('#monthlyCustomerId').val($btn.data('customer-id'));
            $('#monthlyStatementMonth').val($btn.data('month'));
            $('#monthlyCollectSubtitle').text($btn.data('customer') + ' | ' + $btn.data('month') + ' | Due: ' + money($btn.data('due')));
            $('#monthlyAmount').val(money($btn.data('due')));
            $('#monthlyReceiveDate').val($('#creditCollectionPageData').data('today') || '');
            $('#monthlyPaymentMethod').val('CASH');
            $('#monthlyDueDate, #monthlyChequeNumber, #monthlyChequeDate, #monthlyNote').val('');
            syncMonthlyChequeFields();
        });

        $('.js-history').on('click', function () {
            const $btn = $(this);
            $('#paymentHistoryTitle').text('Payment History - ' + $btn.data('invoice'));
            $('#paymentHistoryBody').html('<tr><td colspan="5" class="text-center text-muted py-4">Loading...</td></tr>');
            $.get('/credit-collections/' + $btn.data('id') + '/payments')
                .done(function (rows) {
                    if (!rows || rows.length === 0) {
                        $('#paymentHistoryBody').html('<tr><td colspan="5" class="text-center text-muted py-4">No collection yet.</td></tr>');
                        return;
                    }
                    const html = rows.map(function (row) {
                        const cheque = row.chequeNumber ? escapeHtml(row.chequeNumber) + (row.chequeDate ? ' / ' + escapeHtml(row.chequeDate) : '') : '-';
                        return '<tr>'
                            + '<td class="ps-3">' + escapeHtml(row.receiveDate || '-') + '</td>'
                            + '<td class="text-end fw-semibold">' + money(row.amount) + '</td>'
                            + '<td>' + escapeHtml(row.paymentMethod || '-') + '</td>'
                            + '<td>' + cheque + '</td>'
                            + '<td>' + escapeHtml(row.note || '-') + '</td>'
                            + '</tr>';
                    }).join('');
                    $('#paymentHistoryBody').html(html);
                })
                .fail(function () {
                    $('#paymentHistoryBody').html('<tr><td colspan="5" class="text-center text-danger py-4">Could not load payment history.</td></tr>');
                });
        });

        $('#paymentMethod').on('change', syncChequeFields);
        $('#monthlyPaymentMethod').on('change', syncMonthlyChequeFields);
    });

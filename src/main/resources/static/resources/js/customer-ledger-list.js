$(function () {
    function syncFilters() {
        const monthly = $('#ledgerType').val() === 'monthly';
        $('.js-date-filter').toggle(!monthly);
        $('.js-month-filter').toggle(monthly);
    }

    function syncChequeFields() {
        $('.js-cheque-fields').toggleClass('d-none', $('#paymentMethod').val() !== 'CHEQUE');
    }

    $('#ledgerType').on('change', syncFilters);
    $('#paymentMethod').on('change', syncChequeFields);
    syncFilters();
    syncChequeFields();
});

$(function () {
    function syncStatementFilters() {
        const type = $('#statementType').val();
        $('.js-date-filter').toggle(type === 'daily');
        $('.js-month-filter').toggle(type === 'monthly');
    }

    $('#statementType').on('change', syncStatementFilters);
    syncStatementFilters();
});

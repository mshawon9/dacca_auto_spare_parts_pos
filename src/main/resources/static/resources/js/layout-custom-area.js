$(function () {
    const $brandIds = $('#multiple-select-field');

    $brandIds.select2({
        theme: 'bootstrap-5',
        width: '100%',
        placeholder: 'Select brands',
        dropdownParent: $('#categoryModal'),
        closeOnSelect: false
    });

    $brandIds.on('select2:open', function () {
        setTimeout(function () {
            const $searchField = $('.select2-container--open .select2-search__field');
            $searchField.trigger('focus');
        }, 0);
    });
});

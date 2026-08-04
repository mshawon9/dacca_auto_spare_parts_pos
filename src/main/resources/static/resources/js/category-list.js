$(function () {
    const $brandIds = $('#brandIds');
    const $selectedBrandCount = $('#selectedBrandCount');

    const brandDualList = $brandIds.bootstrapDualListbox({
        nonSelectedListLabel: 'Available brands',
        selectedListLabel: 'Brands in this category',
        preserveSelectionOnMove: 'moved',
        moveOnSelect: false,
        filterPlaceHolder: 'Search brands',
        infoText: 'Showing all {0}',
        infoTextFiltered: '<span class="badge text-bg-warning">Filtered</span> {0} from {1}',
        infoTextEmpty: 'No brands'
    });

    function updateSelectedBrandCount() {
        const count = ($brandIds.val() || []).length;
        $selectedBrandCount.text(count + (count === 1 ? ' selected' : ' selected'));
        $selectedBrandCount.toggleClass('text-bg-primary', count > 0);
        $selectedBrandCount.toggleClass('text-bg-secondary', count === 0);
    }

    $brandIds.on('change', updateSelectedBrandCount);
    updateSelectedBrandCount();

    $('#categoryModal').on('shown.bs.modal', function () {
        brandDualList.bootstrapDualListbox('refresh', true);
        updateSelectedBrandCount();
    });

    window.refreshCategoryBrandPicker = function () {
        brandDualList.bootstrapDualListbox('refresh', true);
        updateSelectedBrandCount();
    };

    const $form = $('#categoryForm');
    const $modalTitle = $('#categoryModalLabel');

    $('#btnCreateCategory').on('click', function () {
        $form.attr('action', '/categories/create');
        $modalTitle.text('Create Category');

        $('#name').val('');
        $('#description').val('');
        $('#active').prop('checked', true);
        $('#brandIds').val([]).trigger('change');
        window.refreshCategoryBrandPicker && window.refreshCategoryBrandPicker();
    });

    $('.btn-edit-category').on('click', function () {
        const id = $(this).data('id');

        $.get('/categories/' + id + '/edit-data', function (data) {
            $form.attr('action', '/categories/' + id + '/edit');
            $modalTitle.text('Edit Category');

            $('#name').val(data.name || '');
            $('#description').val(data.description || '');
            $('#active').prop('checked', data.active === true);

            $('#brandIds').val((data.brandIds || []).map(String)).trigger('change');
            window.refreshCategoryBrandPicker && window.refreshCategoryBrandPicker();
        });
    });
});

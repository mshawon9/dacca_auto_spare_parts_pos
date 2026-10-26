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
    let savingBrand = false;

    function brandMessage(message, error) {
        $('#quickBrandMessage').removeClass('d-none alert-success alert-danger')
            .addClass(error ? 'alert-danger' : 'alert-success').text(message);
    }

    function resetQuickBrand() {
        $('#quickBrandPanel').prop('hidden', true);
        $('#toggleQuickBrand').attr('aria-expanded', 'false');
        $('#quickBrandName, #quickBrandCode').val('').removeClass('is-invalid');
        $('#quickBrandMessage').addClass('d-none').empty();
    }

    function selectBrand(brand, message) {
        let $option = $brandIds.find('option').filter(function () { return this.value === String(brand.id); });
        if (!$option.length) {
            $option = $(new Option(brand.name, brand.id));
            $brandIds.append($option);
        }
        $option.prop('selected', true);
        $brandIds.trigger('change');
        window.refreshCategoryBrandPicker();
        if (!$('#brandId option').filter(function () { return this.value === String(brand.id); }).length) {
            $('#brandId').append(new Option(brand.name, brand.id));
        }
        resetQuickBrand();
        brandMessage(message, false);
    }

    $('#toggleQuickBrand').on('click', function () {
        const open = $('#quickBrandPanel').prop('hidden');
        $('#quickBrandPanel').prop('hidden', !open);
        $(this).attr('aria-expanded', String(open));
        if (open) $('#quickBrandName').trigger('focus');
    });

    $('#quickBrandName, #quickBrandCode').on('keydown', function (event) {
        if (event.key === 'Enter') {
            event.preventDefault();
            $('#saveQuickBrand').trigger('click');
        }
    });

    $('#saveQuickBrand').on('click', function () {
        if (savingBrand) return;
        const name = $('#quickBrandName').val().trim();
        const code = $('#quickBrandCode').val().trim();
        $('#quickBrandName, #quickBrandCode').removeClass('is-invalid');
        if (!name || name.length > 100 || code.length > 30) {
            $('#quickBrandName').toggleClass('is-invalid', !name || name.length > 100);
            $('#quickBrandCode').toggleClass('is-invalid', code.length > 30);
            brandMessage('Enter a brand name (up to 100 characters) and an optional code (up to 30 characters).', true);
            return;
        }
        const existing = $brandIds.find('option').filter(function () {
            return $(this).text().trim().toLowerCase() === name.toLowerCase();
        }).first();
        if (existing.length) {
            selectBrand({id: existing.val(), name: existing.text()}, 'This brand already exists and has been selected.');
            return;
        }
        savingBrand = true;
        $('#saveQuickBrand').prop('disabled', true).text('Saving...');
        $form.find('[type="submit"]').prop('disabled', true);
        $.ajax({
            url: '/brands/quick-create',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({name: name, code: code || null, active: true})
        }).done(function (brand) {
            selectBrand(brand, 'Brand created and selected. Save the category to finish.');
        }).fail(function (xhr) {
            const body = xhr.responseJSON || {};
            const errors = body.fieldErrors || {};
            $('#quickBrandName').toggleClass('is-invalid', !!errors.name);
            $('#quickBrandCode').toggleClass('is-invalid', !!errors.code);
            brandMessage(Object.values(errors).join(' ') || body.error || 'Could not create the brand. Please try again.', true);
        }).always(function () {
            savingBrand = false;
            $('#saveQuickBrand').prop('disabled', false).text('Save & Select');
            $form.find('[type="submit"]').prop('disabled', false);
        });
    });

    $('#categoryModal').on('hide.bs.modal', function (event) {
        if (savingBrand) event.preventDefault();
    });
    $form.on('submit', function (event) {
        if (savingBrand) event.preventDefault();
    });

    $('#btnCreateCategory').on('click', function () {
        resetQuickBrand();
        $form.attr('action', '/categories/create');
        $modalTitle.text('Create Category');

        $('#name').val('');
        $('#description').val('');
        $('#active').prop('checked', true);
        $('#brandIds').val([]).trigger('change');
        window.refreshCategoryBrandPicker && window.refreshCategoryBrandPicker();
    });

    $('.btn-edit-category').on('click', function () {
        resetQuickBrand();
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

    if ($('#categoryPageData').data('show-category-modal') === true) {
        bootstrap.Modal.getOrCreateInstance(document.getElementById('categoryModal')).show();
    }
});

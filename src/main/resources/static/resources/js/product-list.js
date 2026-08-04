$(function () {
    const $category = $('#categoryId');
    const $brand = $('#brandId');

    $('#applicationId').select2({
        theme: 'bootstrap-5',
        width: '100%',
        placeholder: 'All Applications',
        allowClear: true
    });

    $category.on('change', function () {
        const categoryId = $(this).val();

        $brand
            .prop('disabled', true)
            .empty()
            .append('<option value="">' + (categoryId ? 'Loading brands...' : 'All Brands') + '</option>');

        if (!categoryId) {
            $brand.prop('disabled', false);
            return;
        }

        $.get('/api/v1/lookups/brands', {categoryId: categoryId}, function (data) {
            $brand.empty().append('<option value="">All Brands</option>');

            $.each(data, function (_, item) {
                $brand.append($('<option>', {
                    value: item.id,
                    text: item.name
                }));
            });
        }).fail(function () {
            $brand
                .empty()
                .append('<option value="">Could not load brands</option>');
        }).always(function () {
            $brand.prop('disabled', false);
        });
    });
});

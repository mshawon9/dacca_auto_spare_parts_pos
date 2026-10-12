$(function () {
    const $category = $('#categoryId');
    const $brand = $('#brandId');
    const $searchCard = $('#productSearchCard');
    const productSearchCardStateKey = 'productList.searchCardExpanded';

    $('#makeId').select2({
        theme: 'bootstrap-5',
        width: '100%',
        placeholder: 'All Makes',
        allowClear: true
    });

    function setSearchCardState(expanded) {
        const $body = $searchCard.children('.card-body');
        const $icon = $searchCard.find('[data-card-widget="collapse"] i').first();

        $searchCard.toggleClass('collapsed-card', !expanded);
        $body.toggle(expanded);
        $icon
            .toggleClass('fas fa-plus', !expanded)
            .toggleClass('fas fa-minus', expanded);
        localStorage.setItem(productSearchCardStateKey, expanded ? 'expanded' : 'collapsed');
    }

    if ($searchCard.length) {
        setSearchCardState(localStorage.getItem(productSearchCardStateKey) === 'expanded');

        $searchCard.find('[data-card-widget="collapse"]').on('click', function () {
            const wasExpanded = !$searchCard.hasClass('collapsed-card');
            setTimeout(function () {
                const isExpanded = !$searchCard.hasClass('collapsed-card');
                setSearchCardState(isExpanded === wasExpanded ? !wasExpanded : isExpanded);
            }, 0);
        });

        $searchCard
            .on('expanded.lte.cardwidget', function () {
                setSearchCardState(true);
            })
            .on('collapsed.lte.cardwidget', function () {
                setSearchCardState(false);
            });
    }

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

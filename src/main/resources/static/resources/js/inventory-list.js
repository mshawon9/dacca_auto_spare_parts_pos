$(function () {
    const selectedStoreId = $('#inventoryPageData').data('selected-store-id') || null;
    const adjustmentModal = bootstrap.Modal.getOrCreateInstance(document.getElementById('adjustmentModal'));
    const priceModal = bootstrap.Modal.getOrCreateInstance(document.getElementById('priceModal'));
    const priceHistoryModal = bootstrap.Modal.getOrCreateInstance(document.getElementById('priceHistoryModal'));
    let activeRow = null;
    let priceRow = null;

    function showAlert(message, type) {
        $('#inventoryAlert')
            .removeClass('d-none alert-success alert-danger')
            .addClass(type === 'success' ? 'alert-success' : 'alert-danger')
            .text(message);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    function errorMessage(xhr, fallback) {
        const response = xhr.responseJSON || {};
        if (response.fieldErrors) {
            return Object.values(response.fieldErrors).join(' ');
        }
        return response.error || fallback;
    }

    function formatQuantity(value) {
        const number = Number(value);
        return Number.isFinite(number) ? String(Math.trunc(number)) : '0';
    }

    function formatPrice(value) {
        if (value === null || value === undefined || value === '' || !Number.isFinite(Number(value))) {
            return 'Not set';
        }
        return Number(value).toFixed(2);
    }

    function updateSummary() {
        let inStock = 0;
        let outOfStock = 0;
        $('.inventory-row').each(function () {
            Number($(this).attr('data-quantity')) > 0 ? inStock++ : outOfStock++;
        });
        $('#inStockCount').text(inStock);
        $('#outOfStockCount').text(outOfStock);
    }

    function updateRowQuantity($row, quantity) {
        const normalized = Number(quantity);
        $row.attr('data-quantity', normalized);
        $row.find('.stock-quantity')
            .text(formatQuantity(normalized))
            .toggleClass('text-bg-success', normalized > 0)
            .toggleClass('text-bg-danger', normalized <= 0);
        updateSummary();
    }

    function openAdjustment($row, action) {
        activeRow = $row;
        const currentQuantity = Number($row.attr('data-quantity'));
        const productName = $row.attr('data-product-name');
        const labels = {
            INCREASE: { title: 'Increase Stock', quantity: 'Quantity to add', button: 'Increase Stock', className: 'btn-success' },
            DECREASE: { title: 'Decrease Stock', quantity: 'Quantity to remove', button: 'Decrease Stock', className: 'btn-danger' },
            SET: { title: 'Set Stock Quantity', quantity: 'New exact quantity', button: 'Set Quantity', className: 'btn-primary' }
        };
        const config = labels[action];

        $('#adjustmentError').addClass('d-none').empty();
        $('#adjustmentModalTitle').text(config.title);
        $('#adjustmentProductName').text(productName);
        $('#adjustmentCurrentStock').text(formatQuantity(currentQuantity));
        $('#adjustmentProductId').val($row.attr('data-product-id'));
        $('#adjustmentType').val(action);
        $('#adjustmentQuantityLabel').text(config.quantity);
        $('#adjustmentQuantity')
            .val(action === 'SET' ? formatQuantity(currentQuantity) : '1')
            .attr('min', action === 'SET' ? '0' : '1')
            .attr('step', '1');
        $('#adjustmentNote').val('');
        $('#adjustmentQuantityGroup').removeClass('d-none');
        $('#saveAdjustmentButton')
            .removeClass('btn-primary btn-success btn-danger')
            .addClass(config.className)
            .text(config.button);
        adjustmentModal.show();
    }

    function submitAdjustment($row, type, quantity, note) {
        const $buttons = $row.find('button');
        $buttons.prop('disabled', true);

        $.ajax({
            url: '/inventory/adjust',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                storeId: Number(selectedStoreId),
                productId: Number($row.attr('data-product-id')),
                adjustmentType: type,
                quantity: Number(quantity),
                note: note || null
            })
        }).done(function (response) {
            updateRowQuantity($row, response.newQuantity);
            adjustmentModal.hide();
            showAlert('Stock updated successfully. New quantity: ' + formatQuantity(response.newQuantity), 'success');
        }).fail(function (xhr) {
            $('#adjustmentError').text(errorMessage(xhr, 'Could not update stock.')).removeClass('d-none');
            adjustmentModal.show();
        }).always(function () {
            $buttons.prop('disabled', false);
            $('#saveAdjustmentButton').prop('disabled', false);
        });
    }

    $('.js-adjust-stock').on('click', function () {
        openAdjustment($(this).closest('.inventory-row'), $(this).attr('data-action'));
    });

    $('.js-clear-stock').on('click', function () {
        const $row = $(this).closest('.inventory-row');
        if (Number($row.attr('data-quantity')) === 0) {
            showAlert('This product already has zero stock.', 'error');
            return;
        }
        if (!confirm('Set stock to zero for ' + $row.attr('data-product-name') + '?')) {
            return;
        }
        submitAdjustment($row, 'SET', 0, 'Stock removed manually');
    });

    $('#adjustmentForm').on('submit', function (event) {
        event.preventDefault();
        const type = $('#adjustmentType').val();
        const quantity = Number($('#adjustmentQuantity').val());

        if (!Number.isFinite(quantity) || !Number.isInteger(quantity) || quantity < 0 || (type !== 'SET' && quantity <= 0)) {
            $('#adjustmentError').text(type === 'SET'
                ? 'Quantity must be a whole number, zero or greater.'
                : 'Quantity must be a whole number greater than zero.').removeClass('d-none');
            return;
        }

        $('#saveAdjustmentButton').prop('disabled', true);
        submitAdjustment(activeRow, type, quantity, ($('#adjustmentNote').val() || '').trim());
    });

    $('.js-edit-price').on('click', function () {
        priceRow = $(this).closest('.inventory-row');
        const currentPrice = priceRow.attr('data-price');
        const currentCostPrice = priceRow.attr('data-cost-price');
        $('#priceError').addClass('d-none').empty();
        $('#priceProductName').text(priceRow.attr('data-product-name'));
        $('#currentPriceText').text(formatPrice(currentPrice));
        $('#currentCostPriceText').text(formatPrice(currentCostPrice));
        $('#newPrice').val(currentPrice || '');
        $('#newCostPrice').val(currentCostPrice || '');
        $('#priceNote').val('');
        priceModal.show();
    });

    $('#priceForm').on('submit', function (event) {
        event.preventDefault();
        const newPrice = Number($('#newPrice').val());
        const costPriceValue = ($('#newCostPrice').val() || '').trim();
        const newCostPrice = costPriceValue ? Number(costPriceValue) : null;
        if (!Number.isFinite(newPrice) || newPrice <= 0) {
            $('#priceError').text('Price must be greater than zero.').removeClass('d-none');
            return;
        }
        if (newCostPrice !== null && (!Number.isFinite(newCostPrice) || newCostPrice < 0)) {
            $('#priceError').text('Cost price must be zero or greater.').removeClass('d-none');
            return;
        }

        $('#savePriceButton').prop('disabled', true).text('Updating...');
        $.ajax({
            url: '/inventory/price',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                storeId: Number(selectedStoreId),
                productId: Number(priceRow.attr('data-product-id')),
                price: newPrice,
                costPrice: newCostPrice,
                note: ($('#priceNote').val() || '').trim() || null
            })
        }).done(function (response) {
            priceRow.attr('data-price', response.newPrice);
            priceRow.attr('data-cost-price', response.newCostPrice);
            priceRow.find('.current-price').text(formatPrice(response.newPrice));
            priceRow.find('.current-cost-price').text(formatPrice(response.newCostPrice));
            priceModal.hide();
            showAlert('Prices updated successfully.', 'success');
        }).fail(function (xhr) {
            $('#priceError').text(errorMessage(xhr, 'Could not update price.')).removeClass('d-none');
        }).always(function () {
            $('#savePriceButton').prop('disabled', false).text('Update Price');
        });
    });

    $('.js-price-history').on('click', function () {
        const $row = $(this).closest('.inventory-row');
        $('#priceHistoryProductName').text($row.attr('data-product-name'));
        $('#priceHistoryError').addClass('d-none').empty();
        $('#priceHistoryBody').html(
            '<tr><td colspan="6" class="text-center text-muted py-4">Loading price history...</td></tr>'
        );
        priceHistoryModal.show();

        $.get('/inventory/price-history', {
            storeId: selectedStoreId,
            productId: $row.attr('data-product-id')
        }).done(function (items) {
            const $body = $('#priceHistoryBody').empty();
            if (!items.length) {
                $body.append('<tr><td colspan="6" class="text-center text-muted py-4">No price changes recorded.</td></tr>');
                return;
            }

            $.each(items, function (_, item) {
                const $tr = $('<tr>');
                $('<td>', { class: 'ps-3' }).text(new Date(item.changedAt).toLocaleString()).appendTo($tr);
                $('<td>').text(formatPrice(item.oldPrice)).appendTo($tr);
                $('<td>', { class: 'fw-semibold' }).text(formatPrice(item.newPrice)).appendTo($tr);
                $('<td>').text(formatPrice(item.oldCostPrice)).appendTo($tr);
                $('<td>', { class: 'fw-semibold' }).text(formatPrice(item.newCostPrice)).appendTo($tr);
                $('<td>').text(item.note || '-').appendTo($tr);
                $body.append($tr);
            });
        }).fail(function (xhr) {
            $('#priceHistoryBody').empty();
            $('#priceHistoryError').text(errorMessage(xhr, 'Could not load price history.')).removeClass('d-none');
        });
    });

    $('#storeId').on('change', function () {
        if ($(this).val()) {
            $('#inventoryFilterForm').trigger('submit');
        }
    });

    $('#storeForm').on('submit', function (event) {
        event.preventDefault();
        $('#storeError').addClass('d-none').empty();
        $('#saveStoreButton').prop('disabled', true).text('Creating...');

        $.ajax({
            url: '/inventory/stores',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                name: ($('#storeName').val() || '').trim(),
                code: ($('#storeCode').val() || '').trim() || null,
                address: ($('#storeAddress').val() || '').trim() || null
            })
        }).done(function (store) {
            window.location.href = '/inventory?storeId=' + store.id;
        }).fail(function (xhr) {
            $('#storeError').text(errorMessage(xhr, 'Could not create store.')).removeClass('d-none');
        }).always(function () {
            $('#saveStoreButton').prop('disabled', false).text('Create Store');
        });
    });

    updateSummary();
});

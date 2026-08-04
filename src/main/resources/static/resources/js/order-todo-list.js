$(function () {
        const showReorderPanel = $('#orderTodoPageData').data('show-reorder') === true;
        const reorderPanel = document.getElementById('reorderOffcanvas');
        if (showReorderPanel && reorderPanel && window.bootstrap) {
            const reorderOffcanvas = bootstrap.Offcanvas.getOrCreateInstance(reorderPanel);
            reorderOffcanvas.show();
            reorderPanel.addEventListener('hidden.bs.offcanvas', function () {
                const targetUrl = new URL(window.location.href);
                targetUrl.searchParams.delete('showReorder');
                window.location.href = targetUrl.toString();
            }, { once: true });
        }

        const $search = $('#todoProductSearch');
        const $results = $('#todoProductResults');
        const $brandSelect = $('#todoBrandSelect');
        const $selected = $('#selectedTodoProduct');
        const $selectedBrand = $('#selectedTodoBrand');
        const stockModalElement = document.getElementById('todoStockModal');
        const stockModal = stockModalElement && window.bootstrap
            ? new bootstrap.Modal(stockModalElement)
            : null;
        let searchTimer = null;

        function resetBrandSelect(message) {
            $brandSelect.prop('disabled', true)
                .empty()
                .append($('<option>', {value: '', text: 'Select product first'}));
            $selectedBrand.removeClass('text-danger').text(message || 'Choose the brand/part number after selecting product.');
        }

        function loadBrandOptions(productId) {
            resetBrandSelect('Loading brands...');
            $.get('/products/' + productId + '/brand-options-json', function (items) {
                $brandSelect.empty().append($('<option>', {value: '', text: 'Select brand'}));
                if (!items || !items.length) {
                    resetBrandSelect('No brand found for this product.');
                    return;
                }

                $.each(items, function (_, item) {
                    $('<option>', {
                        value: item.id,
                        text: item.text
                    }).appendTo($brandSelect);
                });
                $brandSelect.prop('disabled', false);
                if (items.length === 1) {
                    $brandSelect.val(items[0].id);
                    $selectedBrand.text('Selected: ' + items[0].text);
                } else {
                    $selectedBrand.text('Select a brand from the list.');
                }
            }).fail(function () {
                resetBrandSelect('Could not load brands for this product.');
                $selectedBrand.addClass('text-danger');
            });
        }

        function renderResults(items) {
            $results.empty();
            if (!items || !items.length) {
                $results.removeClass('d-none')
                    .append('<div class="list-group-item text-muted small">No matching product found.</div>');
                return;
            }

            $.each(items, function (_, item) {
                $('<button>', {
                    type: 'button',
                    class: 'list-group-item list-group-item-action js-select-todo-product',
                    text: item.text
                }).attr('data-product-id', item.id).appendTo($results);
            });
            $results.removeClass('d-none');
        }

        $search.on('input', function () {
            const term = ($(this).val() || '').trim();
            $selected.text('No product selected.');
            resetBrandSelect();
            clearTimeout(searchTimer);

            if (term.length < 2) {
                $results.addClass('d-none').empty();
                return;
            }

            searchTimer = setTimeout(function () {
                $.get('/products/search-groups-json', { keyword: term }, renderResults)
                    .fail(function () {
                        $results.empty().removeClass('d-none')
                            .append('<div class="list-group-item text-danger small">Could not load products.</div>');
                    });
            }, 250);
        });

        $results.on('click', '.js-select-todo-product', function () {
            $search.val($(this).text());
            $selected.text('Selected: ' + $(this).text());
            $results.addClass('d-none').empty();
            loadBrandOptions($(this).attr('data-product-id'));
        });

        $brandSelect.on('change', function () {
            const label = $(this).find('option:selected').text();
            if ($(this).val()) {
                $selectedBrand.removeClass('text-danger').text('Selected: ' + label);
            } else {
                $selectedBrand.text('Select a brand from the list.');
            }
        });

        $('#addTodoForm').on('submit', function (event) {
            if (!$brandSelect.val()) {
                event.preventDefault();
                $selectedBrand.text('Please select a brand before adding todo.').addClass('text-danger');
            }
        });

        $('#addTodoModal').on('hidden.bs.modal', function () {
            $search.val('');
            $selected.removeClass('text-danger').text('No product selected.');
            resetBrandSelect();
            $results.addClass('d-none').empty();
        });

        $('.js-open-stock-modal').on('click', function () {
            $('#stockModalError').addClass('d-none').empty();
            $('#stockTodoId').val($(this).attr('data-todo-id'));
            $('#stockProductId').val($(this).attr('data-product-id'));
            $('#stockProductName').text($(this).attr('data-product-name'));
            $('#stockPartNumber').text($(this).attr('data-part-number'));
            $('#stockCurrentQuantity').text($(this).attr('data-current-stock'));
            $('#stockQuantity').val('1');
            if (stockModal) {
                stockModal.show();
            }
        });

        $('#todoStockForm').on('submit', function (event) {
            const quantity = Number($('#stockQuantity').val());
            if (!Number.isFinite(quantity) || !Number.isInteger(quantity) || quantity <= 0) {
                event.preventDefault();
                $('#stockModalError')
                    .text('Quantity must be a whole number greater than zero.')
                    .removeClass('d-none');
            }
        });
    });

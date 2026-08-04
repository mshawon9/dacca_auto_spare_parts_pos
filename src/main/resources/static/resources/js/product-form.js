$(function () {
        const $category = $('#categoryId');
        const $brand = $('#brandId');
        const $barcode = $('#barcode');
        const $generateBarcodeButton = $('#generateBarcodeButton');
        const $alternativePartNumber = $('#alternativePartNumber');
        const $alternativePartNumbersSelect = $('#alternativePartNumbersSelect');
        const $applications = $('#applicationIds');
        const $modalMake = $('#modalVehicleMakeName');
        const $modalMakeId = $('#modalVehicleMakeId');
        const $modalMakeHelp = $('#modalVehicleMakeHelp');
        const $modalModel = $('#modalVehicleModelName');
        const $modalModelId = $('#modalVehicleModelId');
        const $modalModelHelp = $('#modalVehicleModelHelp');
        const $applicationModalForm = $('#applicationModalForm');
        const $applicationModalError = $('#applicationModalError');
        const $saveApplicationButton = $('#saveApplicationButton');
        const $openApplicationModalButton = $('#openApplicationModalButton');
        const $loadLastProductButton = $('#loadLastProductButton');
        const applicationModalElement = document.getElementById('applicationModal');
        const applicationModal = bootstrap.Modal.getOrCreateInstance(applicationModalElement);
        const $selectedApplicationCount = $('#selectedApplicationCount');
        const $similarSearch = $('#similarProductSearch');
        const $similarResults = $('#similarProductResults');
        const $selectedSimilarProduct = $('#selectedSimilarProduct');
        const $noSimilarProduct = $('#noSimilarProduct');
        const productFormPageData = $('#productFormPageData');
        const editMode = productFormPageData.data('edit-mode') === true;
        const currentProductId = productFormPageData.data('current-product-id') || null;
        const categoriesForPrefix = $('#categoryId option').map(function () {
            return { name: ($(this).text() || '').trim() };
        }).get();
        const categoryNames = (categoriesForPrefix || []).map(function (category) {
            return category.name || '';
        });
        let similarSearchTimer = null;
        let suppressCategoryChange = false;
        const placeholderImageUrl = '/img/placeholder-image.png';
        const applicationDualList = $applications.bootstrapDualListbox({
            nonSelectedListLabel: 'Available fitments',
            selectedListLabel: 'Fits this product',
            preserveSelectionOnMove: 'moved',
            moveOnSelect: false,
            filterPlaceHolder: 'Search applications',
            infoText: 'Showing all {0}',
            infoTextFiltered: '<span class="badge text-bg-warning">Filtered</span> {0} from {1}',
            infoTextEmpty: 'No applications'
        });

        $category.select2({
            theme: 'bootstrap-5',
            width: '100%',
            placeholder: 'Select category',
            allowClear: true
        });

        $brand.select2({
            theme: 'bootstrap-5',
            width: '100%',
            placeholder: 'Select brand',
            allowClear: true
        });

        function updateSelectedApplicationCount() {
            const count = ($applications.val() || []).length;
            $selectedApplicationCount.text(count + (count === 1 ? ' selected' : ' selected'));
            $selectedApplicationCount.toggleClass('text-bg-primary', count > 0);
            $selectedApplicationCount.toggleClass('text-bg-secondary', count === 0);
        }

        $applications.on('change', updateSelectedApplicationCount);
        updateSelectedApplicationCount();

        function splitAlternativePartNumbers(value) {
            return (value || '')
                .split(',')
                .map(function (item) { return item.trim(); })
                .filter(function (item, index, items) {
                    return item && items.indexOf(item) === index;
                });
        }

        function syncAlternativePartNumbers() {
            const values = ($alternativePartNumbersSelect.val() || [])
                .map(function (item) { return (item || '').trim(); })
                .filter(Boolean);
            $alternativePartNumber.val(values.join(', '));
        }

        function initAlternativePartNumbers() {
            const values = splitAlternativePartNumbers($alternativePartNumber.val());
            values.forEach(function (value) {
                $alternativePartNumbersSelect.append(new Option(value, value, true, true));
            });

            $alternativePartNumbersSelect.select2({
                theme: 'bootstrap-5',
                tags: true,
                width: '100%',
                placeholder: 'Add alternative part numbers',
                tokenSeparators: [','],
                createTag: function (params) {
                    const term = $.trim(params.term);
                    if (!term) {
                        return null;
                    }
                    return {
                        id: term,
                        text: term,
                        newTag: true
                    };
                },
                templateResult: function (data) {
                    if (data.newTag) {
                        return $('<span></span>')
                            .append(document.createTextNode('Add part number: '))
                            .append($('<strong></strong>').text(data.text));
                    }
                    return data.text;
                }
            });

            $alternativePartNumbersSelect.on('change select2:select select2:unselect', syncAlternativePartNumbers);
            syncAlternativePartNumbers();
        }

        initAlternativePartNumbers();

        function selectedCategoryName() {
            return $category.val() ? (($category.find('option:selected').text() || '').trim()) : '';
        }

        function applyCategoryPrefixToName() {
            const categoryName = selectedCategoryName();
            const $name = $('#name');
            let name = ($name.val() || '').trim();

            if (!categoryName || !name) {
                return;
            }

            $.each(categoryNames || [], function (_, existingCategoryName) {
                const prefix = (existingCategoryName || '').trim();
                if (prefix && name.toLowerCase().startsWith((prefix + ' ').toLowerCase())) {
                    name = name.substring(prefix.length).trim();
                    return false;
                }
            });

            if (name.toLowerCase().startsWith((categoryName + ' ').toLowerCase())) {
                return;
            }

            $name.val(categoryName + ' ' + name);
        }

        function updatePreview() {
            const category = selectedCategoryName();
            const name = ($('#name').val() || '').trim();
            const partNumber = ($('#partNumber').val() || '').trim();
            const position = ($('#position').val() || '').trim();
            const dimension = ($('#dimension').val() || '').trim();

            let parts = [];

            if ($category.val() && category) parts.push(category);
            if (name) parts.push(name);
            if (partNumber) parts.push(partNumber);
            if (position) parts.push(position);
            if (dimension) parts.push(dimension);

            $('#productPreview').text(parts.length ? parts.join(' | ') : '-');
        }

        function removePartNumberSpaces() {
            const $partNumber = $('#partNumber');
            const cleaned = ($partNumber.val() || '').replace(/\s+/g, '');
            if ($partNumber.val() !== cleaned) {
                $partNumber.val(cleaned);
            }
            return cleaned;
        }

        function loadBrands(categoryId, selectedBrandId) {
            $brand.empty().append('<option value="">Select brand</option>');
            $brand.val('').trigger('change');

            if (!categoryId) {
                return $.Deferred().resolve().promise();
            }

            return $.get('/api/v1/lookups/brands', { categoryId: categoryId }, function (data) {
                $.each(data, function (_, item) {
                    const option = $('<option>', {
                        value: item.id,
                        text: item.name
                    });

                    if (selectedBrandId && String(selectedBrandId) === String(item.id)) {
                        option.prop('selected', true);
                    }

                    $brand.append(option);
                });

                $brand.trigger('change');
            });
        }

        function generateBarcode(force) {
            const categoryId = $category.val();

            if (!categoryId) {
                if (force) {
                    alert('Please select a category first.');
                }
                return;
            }

            if (!force && ($barcode.val() || '').trim()) {
                return;
            }

            $generateBarcodeButton.prop('disabled', true).text('Generating...');
            $.get('/products/barcode-suggestion', { categoryId: categoryId }, function (data) {
                $barcode.val(data.barcode || '');
            }).fail(function () {
                if (force) {
                    alert('Could not generate barcode. Please try again.');
                }
            }).always(function () {
                $generateBarcodeButton.prop('disabled', false).text('Generate');
            });
        }


        function renderSimilarProductResults(items) {
            $similarResults.empty();
            const selectedId = String($selectedSimilarProduct.find('.js-similar-chip').attr('data-product-id') || '');
            items = (items || []).filter(function (item) {
                return String(item.id) !== selectedId;
            });

            if (!items || !items.length) {
                $similarResults
                    .removeClass('d-none')
                    .append('<div class="list-group-item text-muted small">No matching product found.</div>');
                return;
            }

            $.each(items, function (_, item) {
                $('<button>', {
                    type: 'button',
                    class: 'list-group-item list-group-item-action js-copy-product',
                    text: item.text
                })
                    .attr('data-product-id', item.id)
                    .appendTo($similarResults);
            });

            $similarResults.removeClass('d-none');
        }

        function searchSimilarProducts(term) {
            $.get('/products/search-json', {
                keyword: term,
                categoryId: $category.val() || '',

                excludeProductId: currentProductId || ''
            }, renderSimilarProductResults).fail(function () {
                $similarResults
                    .empty()
                    .removeClass('d-none')
                    .append('<div class="list-group-item text-danger small">Could not load products.</div>');
            });
        }

        function fillFormFromCopySource(product, options) {
            const copyBrand = options && options.copyBrand === true;

            suppressCategoryChange = true;
            $category.val(product.categoryId ? String(product.categoryId) : '').trigger('change');
            suppressCategoryChange = false;
            loadBrands(product.categoryId, copyBrand ? product.brandId : null);

            $('#name').val(product.name || '');
            $('#position').val(product.position || '');
            $('#dimension').val(product.dimension || '');
            $('#reorderLevel').val(product.reorderLevel || 2);
            $('#description').val(product.description || '');
            $('#active').prop('checked', product.active !== false);

            // These are intentionally cleared because they usually identify the new product uniquely.
            $('#partNumber').val('').removeClass('is-valid is-invalid');
            $('#sku').val('').removeClass('is-valid is-invalid');
            $alternativePartNumbersSelect.val(null).trigger('change');
            $alternativePartNumber.val('').removeClass('is-valid is-invalid');
            $barcode.val('').removeClass('is-valid is-invalid');
            generateBarcode(false);

            const applicationIds = (product.applicationIds || []).map(String);
            $applications.val(applicationIds);
            applicationDualList.bootstrapDualListbox('refresh', true);
            updateSelectedApplicationCount();

            updatePreview();
        }

        function addSimilarProduct(product) {
            const productId = String(product.id);
            if ($selectedSimilarProduct.find('.js-similar-chip[data-product-id="' + productId + '"]').length) {
                return;
            }

            $selectedSimilarProduct.empty();
            const $chip = $('<div>', {
                class: 'badge text-bg-light border text-dark d-flex align-items-center gap-2 py-2 px-3 js-similar-chip'
            }).attr('data-product-id', productId);

            $('<input>', {
                type: 'hidden',
                name: 'similarProductId',
                value: productId
            }).appendTo($chip);
            $('<span>').text(product.displayName).appendTo($chip);
            $('<button>', {
                type: 'button',
                class: 'btn-close js-remove-similar',
                'aria-label': 'Remove'
            }).appendTo($chip);

            $selectedSimilarProduct.append($chip);
            $noSimilarProduct.addClass('d-none');
        }

        $similarSearch.on('input', function () {
            const term = ($(this).val() || '').trim();
            clearTimeout(similarSearchTimer);

            if (term.length < 2) {
                $similarResults.addClass('d-none').empty();
                return;
            }

            similarSearchTimer = setTimeout(function () {
                searchSimilarProducts(term);
            }, 300);
        });

        $similarResults.on('click', '.js-copy-product', function () {
            const productId = $(this).attr('data-product-id');
            const displayName = $(this).text();

            $.get('/products/' + productId + '/copy-source', function (product) {
                addSimilarProduct({ id: product.id, displayName: displayName });
                if (!editMode) {
                    fillFormFromCopySource(product, { copyBrand: false });
                }
                $similarSearch.val('');
                $similarResults.addClass('d-none').empty();
            });
        });

        $loadLastProductButton.on('click', function () {
            $loadLastProductButton.prop('disabled', true).text('Loading last entry...');

            $.get('/products/last-copy-source', function (product) {
                fillFormFromCopySource(product, { copyBrand: true });
            }).fail(function () {
                alert('No previous product entry found.');
            }).always(function () {
                $loadLastProductButton
                    .prop('disabled', false)
                    .html('<i class="bi bi-clock-history me-1"></i> Get Last Entry');
            });
        });

        $selectedSimilarProduct.on('click', '.js-remove-similar', function () {
            $(this).closest('.js-similar-chip').remove();
            $noSimilarProduct.removeClass('d-none');
        });

        $(document).on('click', function (event) {
            if (!$(event.target).closest('#similarProductBox').length) {
                $similarResults.addClass('d-none');
            }
        });

        $category.on('change', function () {
            if (suppressCategoryChange) {
                return;
            }
            loadBrands($(this).val(), null);
            $barcode.val('');
            generateBarcode(false);
            applyCategoryPrefixToName();
            updatePreview();
        });

        $generateBarcodeButton.on('click', function () {
            $barcode.val('');
            generateBarcode(true);
        });

        $('#image').on('change', function () {
            const file = this.files && this.files[0];
            const $error = $('#imageClientError');
            $error.addClass('d-none').empty();

            if (!file) {
                return;
            }
            if (!['image/jpeg', 'image/png'].includes(file.type)) {
                this.value = '';
                $error.text('Only JPEG and PNG images are allowed.').removeClass('d-none');
                return;
            }
            if (file.size > 2 * 1024 * 1024) {
                this.value = '';
                $error.text('Product image must not exceed 2 MB.').removeClass('d-none');
                return;
            }

            $('#removeImage').prop('checked', false);
            $('#productImagePreview').attr('src', URL.createObjectURL(file));
        });

        $('#removeImage').on('change', function () {
            if (this.checked) {
                $('#image').val('');
                $('#productImagePreview').attr('src', placeholderImageUrl);
            }
        });

        function showApplicationModalError(message) {
            $applicationModalError.text(message).removeClass('d-none');
        }

        function cleanupApplicationModalBackdrop() {
            $('.modal-backdrop').remove();
            $('body')
                .removeClass('modal-open')
                .css({
                    overflow: '',
                    paddingRight: ''
                });
        }

        function resetApplicationModalForm() {
            $applicationModalForm[0].reset();
            $modalMake.select2('close');
            $modalModel.select2('close');
            $modalMake.val(null).trigger('change');
            $modalMakeId.val('');
            $modalModel.val(null).trigger('change');
            $modalModelId.val('');
            $modalMakeHelp.text('Search an old make or type a new one, then press Enter.');
            $modalModelHelp.text('Select a make first to search old models, or type a new model.');
            $applicationModalError.addClass('d-none').empty();
            $saveApplicationButton.prop('disabled', false).text('Add Application');
        }

        $openApplicationModalButton.on('click', function () {
            resetApplicationModalForm();
            cleanupApplicationModalBackdrop();
            applicationModal.show();
        });

        $(applicationModalElement).on('hidden.bs.modal', function () {
            resetApplicationModalForm();
            cleanupApplicationModalBackdrop();
        });

        function updateModalMakeState() {
            const value = ($modalMake.val() || '').trim();
            const selectedId = $modalMake.find('option:selected').data('id');

            if (selectedId) {
                $modalMakeId.val(selectedId);
                $modalMakeHelp.text('Existing make selected.');
            } else {
                $modalMakeId.val('');
                $modalMakeHelp.text(value ? 'New make will be created when you save.' : 'Search an old make or type a new one, then press Enter.');
            }
        }

        function updateModalModelState() {
            const value = ($modalModel.val() || '').trim();
            const selectedId = $modalModel.find('option:selected').data('id');

            if (selectedId) {
                $modalModelId.val(selectedId);
                $modalModelHelp.text('Existing model selected.');
            } else {
                $modalModelId.val('');
                $modalModelHelp.text(value ? 'New model will be created when you save.' : 'Select a make first to search old models, or type a new model.');
            }
        }

        function createSelect2Tag(params) {
            const term = $.trim(params.term);
            if (!term) {
                return null;
            }
            return {
                id: term,
                text: term,
                newTag: true
            };
        }

        function renderSelect2Tag(data, label) {
            if (data.newTag) {
                return $('<span></span>').append(document.createTextNode(label + ': ')).append($('<strong></strong>').text(data.text));
            }
            return data.text;
        }

        $modalMake.select2({
            theme: 'bootstrap-5',
            tags: true,
            width: '100%',
            placeholder: 'Search or add make',
            allowClear: true,
            dropdownParent: $('#applicationModal'),
            createTag: createSelect2Tag,
            templateResult: function (data) {
                return renderSelect2Tag(data, 'Add new make');
            }
        });

        $modalModel.select2({
            theme: 'bootstrap-5',
            tags: true,
            width: '100%',
            placeholder: 'Search or add model',
            allowClear: true,
            dropdownParent: $('#applicationModal'),
            createTag: createSelect2Tag,
            templateResult: function (data) {
                return renderSelect2Tag(data, 'Add new model');
            }
        });

        function replaceModalModelOptions(models, selectedName) {
            $modalModel.empty().append(new Option('', '', false, false));
            $.each(models || [], function (_, item) {
                const option = new Option(item.name, item.name, false, selectedName && item.name === selectedName);
                $(option).attr('data-id', item.id);
                $modalModel.append(option);
            });

            if (selectedName && !$modalModel.find('option').filter(function () { return $(this).val() === selectedName; }).length) {
                $modalModel.append(new Option(selectedName, selectedName, true, true));
            }

            $modalModel.val(selectedName || null).trigger('change.select2');
            updateModalModelState();
        }

        function loadModalModelsForMake(selectedName) {
            const makeId = $modalMakeId.val();
            if (!makeId) {
                replaceModalModelOptions([], selectedName || $modalModel.val());
                return;
            }

            $.get('/api/v1/lookups/vehicle-models', { makeId: makeId }, function (data) {
                replaceModalModelOptions(data, selectedName || $modalModel.val());
            }).fail(function () {
                $modalModelHelp.text('Could not load old models. You can still type a new model.');
            });
        }

        $modalMake.on('change select2:select select2:clear', function () {
            updateModalMakeState();
            loadModalModelsForMake(null);
        });

        $modalModel.on('change select2:select select2:clear', updateModalModelState);

        $applicationModalForm.on('submit', function (event) {
            event.preventDefault();
            $applicationModalError.addClass('d-none').empty();
            updateModalMakeState();

            const yearFrom = $('#modalYearFrom').val();
            const yearTo = $('#modalYearTo').val();
            const payload = {
                vehicleMakeId: $modalMakeId.val() ? Number($modalMakeId.val()) : null,
                vehicleModelId: $modalModelId.val() ? Number($modalModelId.val()) : null,
                vehicleMakeName: ($modalMake.val() || '').trim(),
                vehicleModelName: ($modalModel.val() || '').trim(),
                variantLabel: ($('#modalVariantLabel').val() || '').trim() || null,
                yearFrom: yearFrom ? Number(yearFrom) : null,
                yearTo: yearTo ? Number(yearTo) : null,
                active: true
            };

            if (!payload.vehicleMakeName || !payload.vehicleModelName) {
                showApplicationModalError('Make and model are required.');
                return;
            }

            $saveApplicationButton.prop('disabled', true).text('Adding...');

            $.ajax({
                url: '/vehicle-applications/create-json',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(payload)
            }).done(function (application) {
                $applications.find('option[value="' + application.id + '"]').remove();
                $applications.append($('<option>', {
                    value: application.id,
                    text: application.displayName,
                    selected: true
                }));
                applicationDualList.bootstrapDualListbox('refresh', true);
                updateSelectedApplicationCount();
                applicationModal.hide();
            }).fail(function (xhr) {
                const response = xhr.responseJSON || {};
                const fieldErrors = response.fieldErrors || {};
                const message = response.error === 'Validation failed'
                    ? Object.values(fieldErrors).join(' ')
                    : response.error || 'Could not create vehicle application.';
                showApplicationModalError(message);
            }).always(function () {
                $saveApplicationButton.prop('disabled', false).text('Add Application');
            });
        });

        $('#name').on('blur', function () {
            applyCategoryPrefixToName();
            updatePreview();
        });
        $('#name, #dimension').on('input', updatePreview);
        $('#partNumber').on('input blur paste', function () {
            setTimeout(function () {
                removePartNumberSpaces();
                updatePreview();
            }, 0);
        });
        $('#position').on('change', updatePreview);
        updatePreview();

        const initialCategoryId = $category.val();
        const initialBrandId = $brand.val();
        if (initialCategoryId) {
            loadBrands(initialCategoryId, initialBrandId);
            generateBarcode(false);
        }

        $('#productForm').validate({
            errorElement: 'div',
            errorClass: 'invalid-feedback',
            rules: {
                categoryId: { required: true },
                brandId: { required: true },
                name: { required: true, maxlength: 200 },
                partNumber: {
                    required: true,
                    maxlength: 100,
                    pattern: /^[A-Za-z0-9._\/-]+$/
                },
                reorderLevel: { required: true, min: 0, number: true },
                alternativePartNumber: { maxlength: 255 },
                position: { maxlength: 80 },
                dimension: { maxlength: 120 },
                sku: { maxlength: 100 },
                barcode: { maxlength: 64 },
                description: { maxlength: 2000 }
            },
            messages: {
                categoryId: { required: 'Category is required.' },
                brandId: { required: 'Brand is required.' },
                name: {
                    required: 'Product name is required.',
                    maxlength: 'Product name must not exceed 200 characters.'
                },
                partNumber: {
                    required: 'Part number is required.',
                    maxlength: 'Part number must not exceed 100 characters.',
                    pattern: 'Part number cannot contain spaces.'
                },
                reorderLevel: {
                    required: 'Reorder level is required.',
                    min: 'Reorder level cannot be negative.',
                    number: 'Reorder level must be a number.'
                }
            },
            errorPlacement: function (error, element) {
                error.addClass('d-block');
                const existing = element.siblings('.invalid-feedback');
                if (existing.length) {
                    existing.first().replaceWith(error);
                } else {
                    error.insertAfter(element);
                }
            },
            highlight: function (element) {
                $(element).addClass('is-invalid').removeClass('is-valid');
            },
            unhighlight: function (element) {
                $(element).removeClass('is-invalid').addClass('is-valid');
            }
        });

        $('#productForm').on('submit', function () {
            applyCategoryPrefixToName();
            syncAlternativePartNumbers();
        });

        $('#productForm').on('submit', function () {
            removePartNumberSpaces();
        });
    });

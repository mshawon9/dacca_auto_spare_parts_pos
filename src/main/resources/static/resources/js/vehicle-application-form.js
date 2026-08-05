$(function () {
        const $make = $('#vehicleMakeName');
        const $makeId = $('#vehicleMakeId');
        const $makeHelp = $('#vehicleMakeHelp');
        const $model = $('#vehicleModelName');
        const $modelId = $('#vehicleModelId');
        const $modelHelp = $('#vehicleModelHelp');
        const $variant = $('#variantLabel');
        const $yearFrom = $('#yearFrom');
        const $yearTo = $('#yearTo');
        const $preview = $('#displayPreview');
        const originalMakeId = $makeId.val();
        const originalModelId = $modelId.val();

        function updatePreview() {
            const make = ($make.val() || '').trim();
            const model = ($model.val() || '').trim();
            const variant = ($variant.val() || '').trim();
            const yearFrom = ($yearFrom.val() || '').trim();
            const yearTo = ($yearTo.val() || '').trim();
            let text = [make, model, variant].filter(Boolean).join(' ');

            if (yearFrom && !yearTo) {
                text += ' ' + yearFrom + ' Up';
            } else if (yearFrom && yearTo && yearFrom === yearTo) {
                text += ' ' + yearFrom;
            } else if (yearFrom && yearTo) {
                text += ' ' + yearFrom + '-' + yearTo;
            }

            $preview.text(text.trim() || '-');
        }

        function updateMakeState() {
            const value = ($make.val() || '').trim();
            const selectedId = $make.find('option:selected').data('id');

            if (selectedId) {
                $makeId.val(selectedId);
                $makeHelp.text('Existing make selected.');
            } else {
                $makeId.val('');
                $makeHelp.text(value ? 'New make will be created when you save.' : 'Search an old make or type a new one, then press Enter.');
            }
        }

        function updateModelState() {
            const value = ($model.val() || '').trim();
            const selectedId = $model.find('option:selected').data('id');

            if (selectedId) {
                $modelId.val(selectedId);
                $modelHelp.text('Existing model selected.');
            } else if (originalModelId && originalMakeId && $makeId.val() === originalMakeId && value) {
                $modelId.val(originalModelId);
                $modelHelp.text('Current model name will be updated.');
            } else {
                $modelId.val('');
                $modelHelp.text(value ? 'New model will be created when you save.' : 'Select a make first to search old models, or type a new model.');
            }
        }

        function createTag(params) {
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

        function renderNewTag(data, label) {
            if (data.newTag) {
                return $('<span></span>').append(document.createTextNode(label + ': ')).append($('<strong></strong>').text(data.text));
            }
            return data.text;
        }

        $make.select2({
            theme: 'bootstrap-5',
            tags: true,
            width: '100%',
            placeholder: 'Search or add make',
            allowClear: true,
            createTag: createTag,
            templateResult: function (data) {
                return renderNewTag(data, 'Add new make');
            }
        });

        $model.select2({
            theme: 'bootstrap-5',
            tags: true,
            width: '100%',
            placeholder: 'Search or add model',
            allowClear: true,
            createTag: createTag,
            templateResult: function (data) {
                return renderNewTag(data, 'Add new model');
            }
        });

        function replaceModelOptions(models, selectedName) {
            $model.empty().append(new Option('', '', false, false));
            $.each(models || [], function (_, item) {
                const option = new Option(item.name, item.name, false, selectedName && item.name === selectedName);
                $(option).attr('data-id', item.id);
                $model.append(option);
            });

            if (selectedName && !$model.find('option').filter(function () { return $(this).val() === selectedName; }).length) {
                $model.append(new Option(selectedName, selectedName, true, true));
            }

            $model.val(selectedName || null).trigger('change.select2');
            updateModelState();
        }

        function loadModelsForMake(selectedName) {
            const makeId = $makeId.val();
            if (!makeId) {
                replaceModelOptions([], selectedName || $model.val());
                return;
            }

            $.get('/api/v1/lookups/vehicle-models', { makeId: makeId }, function (data) {
                replaceModelOptions(data, selectedName || $model.val());
            }).fail(function () {
                $modelHelp.text('Could not load old models. You can still type a new model.');
            });
        }

        $make.on('change select2:select select2:clear', function () {
            updateMakeState();
            loadModelsForMake(null);
            updatePreview();
        });

        $model.on('change select2:select select2:clear', function () {
            updateModelState();
            updatePreview();
        });

        $variant.on('input', updatePreview);
        $yearFrom.on('input', updatePreview);
        $yearTo.on('input', updatePreview);
        updateMakeState();
        updateModelState();
        updatePreview();

        $('#vehicleApplicationForm').validate({
            errorElement: 'div',
            errorClass: 'invalid-feedback',
            rules: {
                vehicleMakeName: { required: true, maxlength: 100 },
                vehicleModelName: { required: true, maxlength: 100 },
                variantLabel: { maxlength: 100 },
                yearFrom: { min: 1950, max: 2100 },
                yearTo: { min: 1950, max: 2100 }
            },
            messages: {
                vehicleMakeName: { required: 'Make is required.' },
                vehicleModelName: { required: 'Model is required.' },
                variantLabel: { maxlength: 'Variant must not exceed 100 characters.' }
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
    });

$(function () {
    const modal = new bootstrap.Modal(document.getElementById('vehicleModelModal'));
    const $form = $('#vehicleModelForm');
    const $title = $('#vehicleModelModalLabel');

    function resetValidationState() {
        $form.validate().resetForm();
        $form.find('.is-invalid').removeClass('is-invalid');
        $form.find('.is-valid').removeClass('is-valid');
        $form.find('label.error').remove();
    }

    $('#btnCreateVehicleModel').on('click', function () {
        $form.attr('action', '/vehicle-models/create');
        $title.text('Create Vehicle Model');
        $('#modalMakeId').val('');
        $('#name').val('');
        $('#active').prop('checked', true);
        resetValidationState();
    });

    $('.btn-edit-vehicle-model').on('click', function () {
        const $btn = $(this);
        $form.attr('action', '/vehicle-models/' + $btn.data('id') + '/edit');
        $title.text('Edit Vehicle Model');
        $('#modalMakeId').val($btn.data('make-id'));
        $('#name').val($btn.data('name') || '');
        $('#active').prop('checked', $btn.data('active') === true || $btn.data('active') === 'true');
        resetValidationState();
    });

    $form.validate({
        errorElement: 'div',
        errorClass: 'invalid-feedback',
        rules: {
            makeId: { required: true },
            name: { required: true, maxlength: 100 }
        },
        messages: {
            makeId: { required: 'Make is required.' },
            name: {
                required: 'Model name is required.',
                maxlength: 'Model name must not exceed 100 characters.'
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

    if ($('#vehicleModelPageData').data('show-modal') === true) {
        modal.show();
    }
});

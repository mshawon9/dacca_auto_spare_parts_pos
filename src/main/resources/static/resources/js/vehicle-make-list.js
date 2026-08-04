$(function () {
    const modal = new bootstrap.Modal(document.getElementById('vehicleMakeModal'));
    const $form = $('#vehicleMakeForm');
    const $title = $('#vehicleMakeModalLabel');

    function resetValidationState() {
        $form.validate().resetForm();
        $form.find('.is-invalid').removeClass('is-invalid');
        $form.find('.is-valid').removeClass('is-valid');
        $form.find('label.error').remove();
    }

    $('#btnCreateVehicleMake').on('click', function () {
        $form.attr('action', '/vehicle-makes/create');
        $title.text('Create Vehicle Make');
        $('#name').val('');
        $('#active').prop('checked', true);
        resetValidationState();
    });

    $('.btn-edit-vehicle-make').on('click', function () {
        const $btn = $(this);
        $form.attr('action', '/vehicle-makes/' + $btn.data('id') + '/edit');
        $title.text('Edit Vehicle Make');
        $('#name').val($btn.data('name') || '');
        $('#active').prop('checked', $btn.data('active') === true || $btn.data('active') === 'true');
        resetValidationState();
    });

    $form.validate({
        errorElement: 'div',
        errorClass: 'invalid-feedback',
        rules: {
            name: {required: true, maxlength: 100}
        },
        messages: {
            name: {
                required: 'Name is required.',
                maxlength: 'Name must not exceed 100 characters.'
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

    if ($('#vehicleMakePageData').data('show-modal') === true) {
        modal.show();
    }
});

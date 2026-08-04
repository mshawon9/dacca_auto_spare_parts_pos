$(function () {
    const brandModalElement = document.getElementById('brandModal');
    const brandModal = new bootstrap.Modal(brandModalElement);

    const $brandForm = $('#brandForm');
    const $modalTitle = $('#brandModalLabel');

    const $name = $('#name');
    const $code = $('#code');
    const $active = $('#active');

    function resetValidationState() {
        $brandForm.validate().resetForm();
        $brandForm.find('.is-invalid').removeClass('is-invalid');
        $brandForm.find('.is-valid').removeClass('is-valid');
        $brandForm.find('label.error').remove();
    }

    function resetFormForCreate() {
        $brandForm.attr('action', '/brands/create');
        $modalTitle.text('Create Brand');

        $name.val('');
        $code.val('');
        $active.prop('checked', true);

        resetValidationState();
    }

    function fillFormForEdit(id, name, code, active) {
        $brandForm.attr('action', '/brands/' + id + '/edit');
        $modalTitle.text('Edit Brand');

        $name.val(name || '');
        $code.val(code || '');
        $active.prop('checked', active === true || active === 'true');

        resetValidationState();
    }

    $brandForm.validate({
        ignore: [],
        errorElement: 'div',
        errorClass: 'invalid-feedback',
        validClass: 'is-valid',
        rules: {
            name: {
                required: true,
                maxlength: 100
            },
            code: {
                maxlength: 30
            }
        },
        messages: {
            name: {
                required: 'Brand name is required.',
                maxlength: 'Brand name must not exceed 100 characters.'
            },
            code: {
                maxlength: 'Code must not exceed 30 characters.'
            }
        },
        errorPlacement: function (error, element) {
            error.addClass('d-block');
            const existingServerError = element.siblings('.invalid-feedback');
            if (existingServerError.length) {
                existingServerError.first().replaceWith(error);
            } else {
                error.insertAfter(element);
            }
        },
        highlight: function (element) {
            $(element).addClass('is-invalid').removeClass('is-valid');
        },
        unhighlight: function (element) {
            $(element).removeClass('is-invalid').addClass('is-valid');
        },
        submitHandler: function (form) {
            form.submit();
        }
    });

    $('#btnCreateBrand').on('click', resetFormForCreate);

    $('.btn-edit-brand').on('click', function () {
        const $btn = $(this);
        fillFormForEdit(
            $btn.data('id'),
            $btn.data('name'),
            $btn.data('code'),
            $btn.data('active')
        );
    });

    if ($('#brandPageData').data('show-brand-modal') === true) {
        brandModal.show();
    }

    $('#brandModal').on('hidden.bs.modal', resetValidationState);
});

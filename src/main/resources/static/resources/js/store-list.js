$(function () {
    if ($('#storePageData').data('open-store-modal') === true) {
        bootstrap.Modal.getOrCreateInstance(document.getElementById('storeModal')).show();
    }

    function valueOrDash(value) {
        return value ? value : '-';
    }

    $('#btnAddStore').on('click', function () {
        $('#storeForm').attr('action', '/stores');
        $('#storeModalTitle').text('Add Store / Shop');
        $('#storeSubmitButton').text('Add Store');
        $('#name, #code').val('');
        $('#address').val('');
        $('#active').prop('checked', true);
    });

    $('.btn-store-edit').on('click', function () {
        const $btn = $(this);
        $('#storeForm').attr('action', '/stores/' + $btn.data('id') + '/edit');
        $('#storeModalTitle').text('Edit Store / Shop');
        $('#storeSubmitButton').text('Update Store');
        $('#name').val($btn.data('name') || '');
        $('#code').val($btn.data('code') || '');
        $('#address').val($btn.data('address') || '');
        $('#active').prop('checked', String($btn.data('active')) === 'true');
    });

    $('.btn-store-details').on('click', function () {
        const $btn = $(this);
        $('#storeDetailName').text(valueOrDash($btn.data('name')));
        $('#storeDetailCode').text(valueOrDash($btn.data('code')));
        $('#storeDetailAddress').text(valueOrDash($btn.data('address')));
        $('#storeDetailStatus').text(String($btn.data('active')) === 'true' ? 'Active' : 'Inactive');
        $('#storeDetailCreated').text(valueOrDash($btn.data('created-at')));
        $('#storeDetailUpdated').text(valueOrDash($btn.data('updated-at')));
    });
});

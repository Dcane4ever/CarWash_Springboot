// Initialize date range picker
document.addEventListener('DOMContentLoaded', function() {
    flatpickr("#dateRange", {
        mode: "range",
        dateFormat: "Y-m-d"
    });

    let changedBookings = new Set();

    // Track changes in status
    $('.status-select').change(function() {
        changedBookings.add($(this).data('id'));
        markAsChanged(this);
    });

    // Track changes in payment checkbox
    $('.payment-checkbox').change(function() {
        changedBookings.add($(this).data('id'));
        markAsChanged(this);
    });

    // Handle Update All Changes button
    $('#updateAllButton').click(function() {
        if (changedBookings.size === 0) {
            alert('No changes to update');
            return;
        }

        const updates = Array.from(changedBookings).map(id => {
            return {
                id: id,
                status: $(".status-select[data-id='" + id + "']").val(),
                isPaid: $(".payment-checkbox[data-id='" + id + "']").is(':checked')
            };
        });

        // Send updates to server
        var contextPath = window.contextPath || '';
        // Remove trailing slash to prevent double slashes
        if (contextPath.endsWith('/')) {
            contextPath = contextPath.slice(0, -1);
        }
        
        $.ajax({
            url: contextPath + '/admin/bookings/bulk-update',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(updates),
            success: function(response) {
                alert('Updates saved successfully');
                changedBookings.clear();
                location.reload(); // Refresh the page to show updated data
            },
            error: function(xhr, status, error) {
                alert('Error saving updates: ' + error);
            }
        });
    });
});

// Visual indication of changed rows
function markAsChanged(element) {
    $(element).closest('tr').addClass('table-warning');
}
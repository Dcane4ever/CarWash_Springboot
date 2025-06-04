var contextPath = window.contextPath || '';

function confirmModify(event) {
    event.preventDefault();
    if (confirm('Are you sure you want to modify this booking?')) {
        event.target.submit();
    }
    return false;
} 
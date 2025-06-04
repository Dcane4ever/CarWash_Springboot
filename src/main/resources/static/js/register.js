var contextPath = window.contextPath || '';

document.addEventListener('DOMContentLoaded', function() {
    // Fade in on page load
    document.body.classList.add('page-transition');
    
    // Handle links with fade transition
    document.querySelectorAll('a').forEach(link => {
        link.addEventListener('click', function(e) {
            if (this.href && !this.href.includes('#')) {
                e.preventDefault();
                const href = this.href;
                
                // Fade out
                document.body.classList.add('fade-out');
                
                // Navigate after fade
                setTimeout(() => {
                    window.location.href = href;
                }, 500);
            }
        });
    });

    // Handle form submissions with fade
    document.querySelector('form').addEventListener('submit', function(e) {
        e.preventDefault();
        document.body.classList.add('fade-out');
        setTimeout(() => {
            this.submit();
        }, 500);
    });
});

function resendVerification() {
    const email = document.querySelector('input[name="email"]').value;
    if (email) {
        fetch(contextPath + '/user/resend-verification?email=' + encodeURIComponent(email), {
            method: 'POST'
        })
        .then(response => response.json())
        .then(data => {
            alert(data.message);
        })
        .catch(error => {
            alert('Error resending verification email');
        });
    }
} 
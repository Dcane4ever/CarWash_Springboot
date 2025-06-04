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
                    if (href.startsWith('/')) {
                        window.location.href = (typeof contextPath !== 'undefined' ? contextPath.replace(/\/$/, '') : '') + href;
                    } else {
                        window.location.href = href;
                    }
                }, 500);
            }
        });
    });

    // Handle form submission with fade effect
    document.querySelector('form').addEventListener('submit', function(e) {
        e.preventDefault();
        document.body.classList.add('fade-out');
        setTimeout(() => {
            this.submit();
        }, 500);
    });
}); 
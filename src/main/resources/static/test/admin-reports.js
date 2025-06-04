/*
// Initialize date picker
document.addEventListener('DOMContentLoaded', function() {
    // Fetch and render charts
    fetchReportData();
});

function fetchReportData() {
    // Get params from the form or URL
    const urlParams = new URLSearchParams(window.location.search);
    const reportType = urlParams.get('reportType') || 'daily';
    const startDate = urlParams.get('startDate') || new Date().toISOString().slice(0, 10);

    fetch(`/api/admin/report-data?reportType=${reportType}&startDate=${startDate}`)
        .then(response => response.json())
        .then(data => {
            initializeRevenueChart(data.dailyRevenue || {});
            initializeServiceChart(data.serviceDistribution || {});
});
}

function initializeRevenueChart(revenueData) {
    const ctx = document.getElementById('revenueChart').getContext('2d');
    // Destroy previous chart if exists
    if (window.revenueChartInstance) window.revenueChartInstance.destroy();

    const dates = Object.keys(revenueData).sort();
    const revenues = dates.map(date => parseFloat(revenueData[date]));

    if (!dates.length) return;

    window.revenueChartInstance = new Chart(ctx, {
            type: 'line',
            data: {
            labels: dates,
                datasets: [{
                label: 'Revenue',
                data: revenues,
                borderColor: 'rgb(75, 192, 192)',
                tension: 0.1,
                fill: false
                }]
            },
            options: {
                responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(value) {
                            return '₱' + value.toLocaleString();
                        }
                    }
                }
            },
            plugins: {
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return '₱' + context.raw.toLocaleString();
                        }
                    }
                }
            }
            }
        });
    }

function initializeServiceChart(serviceData) {
    const ctx = document.getElementById('serviceChart').getContext('2d');
    // Destroy previous chart if exists
    if (window.serviceChartInstance) window.serviceChartInstance.destroy();

    const services = Object.keys(serviceData);
    const counts = services.map(service => parseInt(serviceData[service]));

    if (!services.length) return;

    window.serviceChartInstance = new Chart(ctx, {
            type: 'pie',
            data: {
            labels: services,
                datasets: [{
                data: counts,
                backgroundColor: [
                    'rgb(255, 99, 132)',
                    'rgb(54, 162, 235)',
                    'rgb(255, 205, 86)',
                    'rgb(75, 192, 192)'
                ]
                }]
            },
            options: {
                responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'right'
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.raw || 0;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = Math.round((value / total) * 100);
                            return `${label}: ${value} (${percentage}%)`;
                        }
                    }
                }
            }
            }
        });
    }
*/ 
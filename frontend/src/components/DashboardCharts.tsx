import React, { useState } from 'react';
import { Bar, Doughnut } from 'react-chartjs-2';
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    BarElement,
    ArcElement,
    Title,
    Tooltip,
    Legend,
} from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, Title, Tooltip, Legend);

interface DashboardChartsProps {
    numberFrequency: Record<string, number>;
    oddCount: number;
    evenCount: number;
    zoneDistribution: Record<string, number>;
    sumBins: Record<string, number>;
    lastDigitDistribution: Record<string, number>;
}

type TabKey = 'frequency' | 'oddEven' | 'zones' | 'sum' | 'lastDigit';

const DashboardCharts: React.FC<DashboardChartsProps> = ({
    numberFrequency,
    oddCount,
    evenCount,
    zoneDistribution,
    sumBins,
    lastDigitDistribution,
}) => {
    const [activeTab, setActiveTab] = useState<TabKey>('frequency');

    const tabs: { key: TabKey; label: string }[] = [
        { key: 'frequency', label: 'Frequency' },
        { key: 'oddEven', label: 'OddEven' },
        { key: 'zones', label: 'Zones' },
        { key: 'sum', label: 'Sum' },
        { key: 'lastDigit', label: 'LastDigit' },
    ];

    const chartOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: { labels: { color: '#ccc' } },
        },
        scales: {
            x: { ticks: { color: '#999' }, grid: { color: '#333' } },
            y: { ticks: { color: '#999' }, grid: { color: '#333' } },
        },
    };

    const doughnutOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: { labels: { color: '#ccc' } },
        },
    };

    const renderChart = () => {
        switch (activeTab) {
            case 'frequency':
                return (
                    <div style={{ height: 350 }}>
                        <h5 style={{ color: '#69c8f2', marginBottom: 16 }}>Frequency (1~45)</h5>
                        <Bar
                            data={{
                                labels: Object.keys(numberFrequency),
                                datasets: [{
                                    label: 'Count',
                                    data: Object.values(numberFrequency),
                                    backgroundColor: '#3b82f6',
                                    borderRadius: 2,
                                }],
                            }}
                            options={chartOptions}
                        />
                    </div>
                );

            case 'oddEven':
                return (
                    <div style={{ height: 350, maxWidth: 400, margin: '0 auto' }}>
                        <h5 style={{ color: '#69c8f2', marginBottom: 16 }}>Odd / Even</h5>
                        <Doughnut
                            data={{
                                labels: ['Odd', 'Even'],
                                datasets: [{
                                    data: [oddCount, evenCount],
                                    backgroundColor: ['#fbc400', '#3b82f6'],
                                    borderWidth: 0,
                                }],
                            }}
                            options={doughnutOptions}
                        />
                    </div>
                );

            case 'zones':
                return (
                    <div style={{ height: 350 }}>
                        <h5 style={{ color: '#69c8f2', marginBottom: 16 }}>Zones</h5>
                        <Bar
                            data={{
                                labels: Object.keys(zoneDistribution),
                                datasets: [{
                                    label: 'Zone Count',
                                    data: Object.values(zoneDistribution),
                                    backgroundColor: '#22c55e',
                                    borderRadius: 4,
                                }],
                            }}
                            options={chartOptions}
                        />
                    </div>
                );

            case 'sum':
                return (
                    <div style={{ height: 350 }}>
                        <h5 style={{ color: '#69c8f2', marginBottom: 16 }}>Sum bins</h5>
                        <Bar
                            data={{
                                labels: Object.keys(sumBins),
                                datasets: [{
                                    label: 'Draw Count',
                                    data: Object.values(sumBins),
                                    backgroundColor: '#06b6d4',
                                    borderRadius: 4,
                                }],
                            }}
                            options={chartOptions}
                        />
                    </div>
                );

            case 'lastDigit':
                return (
                    <div style={{ height: 350 }}>
                        <h5 style={{ color: '#69c8f2', marginBottom: 16 }}>Last digit</h5>
                        <Bar
                            data={{
                                labels: Object.keys(lastDigitDistribution),
                                datasets: [{
                                    label: 'Count',
                                    data: Object.values(lastDigitDistribution),
                                    backgroundColor: '#f43f5e',
                                    borderRadius: 4,
                                }],
                            }}
                            options={chartOptions}
                        />
                    </div>
                );
        }
    };

    return (
        <div className="card" style={{ backgroundColor: '#1e293b', border: '1px solid #334155' }}>
            <div className="card-header" style={{ backgroundColor: '#0f172a', borderBottom: '1px solid #334155' }}>
                <h3 className="card-title" style={{ fontWeight: 700, color: '#f1f5f9' }}>Charts</h3>
            </div>
            <div className="card-body">
                <div style={{ marginBottom: 16, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                    {tabs.map((tab) => (
                        <button
                            key={tab.key}
                            className={`btn btn-sm ${activeTab === tab.key ? 'btn-primary' : 'btn-outline-secondary'}`}
                            onClick={() => setActiveTab(tab.key)}
                            style={{
                                transition: 'all 0.2s ease',
                            }}
                        >
                            {tab.label}
                        </button>
                    ))}
                </div>
                {renderChart()}
            </div>
        </div>
    );
};

export default DashboardCharts;

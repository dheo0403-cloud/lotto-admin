import React, { useEffect, useState } from 'react';
import { getDashboardStats, syncDraw } from '../services/api';
import type { DashboardStats } from '../services/api';
import LottoBall from '../components/LottoBall';
import DashboardCharts from '../components/DashboardCharts';
import StrategyScorecard from '../components/StrategyScorecard';

const Dashboard: React.FC = () => {
    const [stats, setStats] = useState<DashboardStats | null>(null);
    const [loading, setLoading] = useState(false);
    const [syncing, setSyncing] = useState(false);

    useEffect(() => {
        loadStats();
    }, []);

    const loadStats = async () => {
        setLoading(true);
        try {
            const res = await getDashboardStats();
            setStats(res.data);
        } catch (e) {
            console.error(e);
        } finally {
            setLoading(false);
        }
    };

    const handleSync = async () => {
        setSyncing(true);
        try {
            await syncDraw(0);
            alert('동기화를 시작했습니다. 잠시 후 로그를 확인해주세요.');
            loadStats();
        } catch (e) {
            alert('동기화에 실패했습니다.');
        } finally {
            setSyncing(false);
        }
    };

    if (loading && !stats) {
        return (
            <div className="text-center p-5">
                <i className="fas fa-spinner fa-spin fa-2x"></i>
                <p className="mt-2 text-light">대시보드 로딩 중...</p>
            </div>
        );
    }

    return (
        <div style={{ paddingBottom: '50px' }}>
            {/* Header */}
            <div className="content-header">
                <div className="container-fluid">
                    <div className="row mb-2">
                        <div className="col-sm-6">
                            <h1 className="m-0" style={{ color: '#f1f5f9', fontWeight: 700 }}>대시보드</h1>
                        </div>
                    </div>
                </div>
            </div>

            {stats && (
                <div className="container-fluid">
                    {/* Row 1: Latest Result & Stats Info */}
                    <div className="row mb-4 align-items-stretch">
                        {/* Latest Draw (Left) */}
                        <div className="col-lg-8">
                            <div className="card h-100" style={{ backgroundColor: '#1e293b', border: 'none', margin: 0 }}>
                                <div className="card-body d-flex flex-column justify-content-center">
                                    <div className="d-flex justify-content-between align-items-center">
                                        <div>
                                            <span style={{ color: '#94a3b8', fontSize: '14px' }}>최신 회차</span>
                                            <h1 style={{ color: '#f1f5f9', fontWeight: 900, fontSize: '48px', margin: '0' }}>
                                                {stats.latestDrawNo}회
                                            </h1>
                                            <span style={{ color: '#64748b' }}>{stats.latestDrawDate}</span>
                                        </div>
                                        <div className="text-right">
                                            <div style={{ marginBottom: '10px' }}>
                                                <span style={{ color: '#94a3b8', fontSize: '13px', marginRight: '10px' }}>당첨번호</span>
                                                {stats.latestNumbers?.map((num, i) => (
                                                    <LottoBall key={i} number={num} size="lg" variant="solid" />
                                                ))}
                                                <span style={{ color: '#64748b', margin: '0 8px', fontSize: '20px' }}>+</span>
                                                <LottoBall number={stats.latestBonus ?? 0} size="lg" variant="solid" />
                                            </div>
                                            <div style={{ fontSize: '11px', color: '#f1f5f9' }}>
                                                <span className="mr-2">● 노(1~10)</span>
                                                <span className="mr-2">● 파(11~20)</span>
                                                <span className="mr-2">● 빨(21~30)</span>
                                                <span className="mr-2">● 회(31~40)</span>
                                                <span>● 초(41~45)</span>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Summary Info Boxes (Right) */}
                        <div className="col-lg-4 d-flex flex-column justify-content-between">
                            <div className="info-box mb-3 flex-fill" style={{ backgroundColor: '#1e293b', border: 'none', color: '#f1f5f9' }}>
                                <span className="info-box-icon bg-info"><i className="fas fa-database"></i></span>
                                <div className="info-box-content">
                                    <span className="info-box-text" style={{ color: '#94a3b8' }}>전체 회차</span>
                                    <span className="info-box-number" style={{ fontSize: '24px' }}>{stats.totalDraws}회</span>
                                </div>
                            </div>
                            <div className="info-box mb-0 flex-fill" style={{ backgroundColor: '#1e293b', border: 'none', color: '#f1f5f9' }}>
                                <span className="info-box-icon bg-warning"><i className="fas fa-trophy"></i></span>
                                <div className="info-box-content">
                                    <span className="info-box-text" style={{ color: '#94a3b8' }}>1등 당첨금</span>
                                    <span className="info-box-number" style={{ color: '#fbbf24', fontSize: '18px' }}>
                                        {stats.latestPrize?.toLocaleString()}원
                                    </span>
                                    <small style={{ color: '#64748b' }}>당첨자: {stats.latestWinnerCount}명</small>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Row 2: Prediction Summary & Next Predictions */}
                    <div className="row mb-4 align-items-stretch">
                        {/* Prediction Summary for Latest Draw (Left) */}
                        <div className="col-lg-8">
                            <div className="card h-100" style={{ backgroundColor: '#1e293b', border: 'none', margin: 0 }}>
                                <div className="card-header" style={{ borderBottom: '1px solid #334155', backgroundColor: 'transparent' }}>
                                    <h3 className="card-title" style={{ fontSize: '13px', color: '#94a3b8' }}>
                                        예측 결과 요약 (전략 / 예측 / 일치 / 보너스 / 등수)
                                    </h3>
                                </div>
                                <div className="card-body p-0 table-responsive">
                                    <table className="table table-dark table-striped mb-0 h-100">
                                        <thead>
                                            <tr>
                                                <th style={{ border: 'none', color: '#94a3b8', fontSize: '13px' }}>전략</th>
                                                <th style={{ border: 'none', color: '#94a3b8', fontSize: '13px' }}>예측 번호 (일치: 금링 / 보너스번호 포함: 흰링)</th>
                                                <th style={{ border: 'none', color: '#94a3b8', fontSize: '13px', textAlign: 'center' }}>일치</th>
                                                <th style={{ border: 'none', color: '#94a3b8', fontSize: '13px', textAlign: 'center' }}>보너스</th>
                                                <th style={{ border: 'none', color: '#94a3b8', fontSize: '13px', textAlign: 'center' }}>등수</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {stats.latestDrawRecommendations.map((rec, i) => {
                                                const rankColor = rec.resultLabel === '꽝' ? '#64748b' : '#34d399';
                                                return (
                                                    <tr key={i} style={{ backgroundColor: i % 2 === 0 ? '#1e293b' : '#1a2234' }}>
                                                        <td style={{ verticalAlign: 'middle', fontWeight: 800, color: '#f1f5f9' }}>{rec.strategy}</td>
                                                        <td style={{ verticalAlign: 'middle' }}>
                                                            {rec.numbers.map((num, j) => {
                                                                const isMatch = stats.latestNumbers?.includes(num) || false;
                                                                const isBonus = stats.latestBonus === num;
                                                                const variant = isMatch ? 'match' : isBonus ? 'bonus' : 'default';
                                                                return (
                                                                    <LottoBall key={j} number={num} size="sm" variant={variant} />
                                                                );
                                                            })}
                                                        </td>
                                                        <td style={{ verticalAlign: 'middle', textAlign: 'center', color: '#f1f5f9' }}>{rec.matchCount}</td>
                                                        <td style={{ verticalAlign: 'middle', textAlign: 'center' }}>
                                                            <span className={`badge ${rec.bonusMatch ? 'badge-light' : 'badge-dark'}`} style={{ opacity: 0.6 }}>
                                                                {rec.bonusMatch ? 'Y' : 'N'}
                                                            </span>
                                                        </td>
                                                        <td style={{ verticalAlign: 'middle', textAlign: 'center' }}>
                                                            <span className="badge" style={{ backgroundColor: rankColor, color: '#fff', padding: '5px 10px', minWidth: '40px' }}>
                                                                {rec.resultLabel}
                                                            </span>
                                                        </td>
                                                    </tr>
                                                );
                                            })}
                                            {stats.latestDrawRecommendations.length === 0 && (
                                                <tr>
                                                    <td colSpan={5} className="text-center p-4 text-muted">선택된 회차에 대한 예측 데이터가 없습니다.</td>
                                                </tr>
                                            )}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>

                        {/* Next Draw Predictions (Right) */}
                        <div className="col-lg-4">
                            <div className="card h-100 mb-0" style={{ backgroundColor: '#1e293b', border: 'none' }}>
                                <div className="card-header" style={{ borderBottom: '1px solid #334155', backgroundColor: 'transparent' }}>
                                    <h3 className="card-title" style={{ fontSize: '14px', color: '#f1f5f9', fontWeight: 700 }}>
                                        다음 회차 예측 {(stats.latestDrawNo || 0) + 1}회
                                    </h3>
                                    <div className="card-tools">
                                        <small style={{ color: '#64748b' }}>저장된 예측 {stats.nextDrawRecommendations.length}건</small>
                                    </div>
                                </div>
                                <div className="card-body p-0">
                                    <table className="table table-dark mb-0 h-100">
                                        <thead>
                                            <tr>
                                                <th style={{ border: 'none', color: '#94a3b8', fontSize: '12px' }}>전략</th>
                                                <th style={{ border: 'none', color: '#94a3b8', fontSize: '12px' }}>번호</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {stats.nextDrawRecommendations.map((rec, i) => (
                                                <tr key={i} style={{ borderBottom: '1px solid #334155' }}>
                                                    <td style={{ verticalAlign: 'middle', fontSize: '12px', fontWeight: 700, color: '#f1f5f9' }}>{rec.strategy}</td>
                                                    <td style={{ verticalAlign: 'middle' }}>
                                                        {rec.numbers.map((num, j) => (
                                                            <LottoBall key={j} number={num} size="sm" />
                                                        ))}
                                                    </td>
                                                </tr>
                                            ))}
                                            {stats.nextDrawRecommendations.length === 0 && (
                                                <tr>
                                                    <td colSpan={2} className="text-center p-3 text-muted" style={{ fontSize: '12px' }}>
                                                        차기 회차 예측이 없습니다.
                                                    </td>
                                                </tr>
                                            )}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Row 3: 전략별 성적 */}
                    <div className="row mb-4">
                        <div className="col-12">
                            <StrategyScorecard />
                        </div>
                    </div>

                    {/* Action Row - Centered Sync Button */}
                    <div className="row mt-2 mb-4">
                        <div className="col-12 text-center">
                            <hr style={{ borderColor: '#334155', borderWidth: '1px', marginBottom: '20px', opacity: 0.3 }} />
                            <div className="d-flex justify-content-center">
                                <button
                                    className="btn btn-primary"
                                    onClick={handleSync}
                                    disabled={syncing}
                                    style={{ 
                                        padding: '12px 60px', 
                                        fontWeight: 800, 
                                        fontSize: '18px',
                                        borderRadius: '12px',
                                        boxShadow: '0 8px 25px rgba(0, 123, 255, 0.4)',
                                        minWidth: '350px',
                                        transition: 'all 0.3s ease'
                                    }}
                                >
                                    <i className={`fas ${syncing ? 'fa-spinner fa-spin' : 'fa-sync-alt'} mr-2`}></i>
                                    {syncing ? '최신 회차 동기화 중...' : '최신 회차 동기화'}
                                </button>
                            </div>
                        </div>
                    </div>

                    {/* Charts Section */}
                    <div className="row mt-4">
                        <div className="col-12">
                            <DashboardCharts
                                numberFrequency={stats.numberFrequency}
                                oddCount={stats.oddCount}
                                evenCount={stats.evenCount}
                                zoneDistribution={stats.zoneDistribution}
                                sumBins={stats.sumBins}
                                lastDigitDistribution={stats.lastDigitDistribution}
                            />
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Dashboard;

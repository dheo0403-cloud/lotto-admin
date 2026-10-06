import React, { useEffect, useState } from 'react';
import { getStrategyStats } from '../services/api';
import type { StrategyStat } from '../services/api';

// 6개를 고를 때 무작위 기대 일치 개수 = 6 × 6 / 45
const EXPECTED_MATCH = (6 * 6) / 45;

const th: React.CSSProperties = { border: 'none', color: '#94a3b8', fontSize: '13px', textAlign: 'center' };
const td: React.CSSProperties = { verticalAlign: 'middle', textAlign: 'center', color: '#f1f5f9' };

const StrategyScorecard: React.FC = () => {
    const [stats, setStats] = useState<StrategyStat[] | null>(null);
    const [error, setError] = useState(false);

    useEffect(() => {
        getStrategyStats().then(res => setStats(res.data)).catch(() => setError(true));
    }, []);

    return (
        <div className="card mb-0" style={{ backgroundColor: '#1e293b', border: 'none' }}>
            <div className="card-header" style={{ borderBottom: '1px solid #334155', backgroundColor: 'transparent' }}>
                <h3 className="card-title" style={{ fontSize: '14px', color: '#f1f5f9', fontWeight: 700 }}>
                    전략별 성적 (채점 완료 건)
                </h3>
                <div className="card-tools">
                    <small style={{ color: '#64748b' }}>무작위 기대 일치 {EXPECTED_MATCH.toFixed(2)}개</small>
                </div>
            </div>
            <div className="card-body p-0 table-responsive">
                <table className="table table-dark table-striped mb-0">
                    <thead>
                        <tr>
                            <th style={{ ...th, textAlign: 'left' }}>전략</th>
                            <th style={th}>채점 세트</th>
                            <th style={th}>평균 일치</th>
                            <th style={th}>기대값 대비</th>
                            <th style={th}>5등 이상</th>
                        </tr>
                    </thead>
                    <tbody>
                        {stats?.map(s => {
                            const diff = s.avgMatch - EXPECTED_MATCH;
                            return (
                                <tr key={s.strategy}>
                                    <td style={{ ...td, textAlign: 'left', fontWeight: 700 }}>{s.strategy}</td>
                                    <td style={td}>{s.total}</td>
                                    <td style={td}>{s.avgMatch.toFixed(2)}</td>
                                    <td style={{ ...td, color: diff >= 0 ? '#34d399' : '#f87171' }}>
                                        {diff >= 0 ? '+' : ''}{diff.toFixed(2)}
                                    </td>
                                    <td style={td}>{s.wins}</td>
                                </tr>
                            );
                        })}
                        {(error || stats?.length === 0) && (
                            <tr>
                                <td colSpan={5} className="text-center p-3 text-muted">
                                    {error ? '성적을 불러오지 못했습니다.' : '채점된 추천이 아직 없습니다.'}
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default StrategyScorecard;

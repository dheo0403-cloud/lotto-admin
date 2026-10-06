import React, { useState } from 'react';
import { generateRecommendations } from '../services/api';
import LottoBall from '../components/LottoBall';

const Recommendation: React.FC = () => {
    const [recommendations, setRecommendations] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);

    const handleGenerate = async () => {
        setLoading(true);
        try {
            const res = await generateRecommendations(5);
            setRecommendations(res.data);
        } catch (e) {
            console.error(e);
            alert('추천 번호 생성에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <div className="content-header">
                <h1>번호 생성</h1>
            </div>
            <div className="card">
                <div className="card-header">
                    <button className="btn btn-success" onClick={handleGenerate} disabled={loading}>
                        {loading ? '생성 중...' : '5세트 추천 번호 생성'}
                    </button>
                </div>
                <div className="card-body p-0">
                    <table className="table table-striped">
                        <thead>
                            <tr>
                                <th>대상 회차</th>
                                <th>전략</th>
                                <th>추천 번호</th>
                                <th>생성일시</th>
                            </tr>
                        </thead>
                        <tbody>
                            {recommendations.map(r => (
                                <tr key={r.id}>
                                    <td>{r.targetDraw}회</td>
                                    <td><span className="badge badge-info">{r.strategy || '기본'}</span></td>
                                    <td>
                                        <LottoBall number={r.num1} size="sm" />
                                        <LottoBall number={r.num2} size="sm" />
                                        <LottoBall number={r.num3} size="sm" />
                                        <LottoBall number={r.num4} size="sm" />
                                        <LottoBall number={r.num5} size="sm" />
                                        <LottoBall number={r.num6} size="sm" />
                                    </td>
                                    <td>{new Date(r.createdAt).toLocaleString()}</td>
                                </tr>
                            ))}
                            {recommendations.length === 0 && (
                                <tr>
                                    <td colSpan={4} className="text-center">아직 생성된 추천 번호가 없습니다.</td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default Recommendation;

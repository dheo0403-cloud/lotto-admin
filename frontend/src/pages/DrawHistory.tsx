import React, { useEffect, useState } from 'react';
import { getDraws } from '../services/api';

const DrawHistory: React.FC = () => {
    const [draws, setDraws] = useState<any[]>([]);

    useEffect(() => {
        loadDraws();
    }, []);

    const loadDraws = async () => {
        try {
            const res = await getDraws(0, 50);
            setDraws(res.data.content);
        } catch (e) {
            console.error(e);
        }
    };

    return (
        <div>
            <div className="content-header">
                <h1>추첨 이력</h1>
            </div>
            <div className="card">
                <div className="card-body p-0">
                    <table className="table table-striped">
                        <thead>
                            <tr>
                                <th>회차</th>
                                <th>추첨일</th>
                                <th>당첨번호</th>
                                <th>보너스</th>
                                <th>당첨금</th>
                                <th>당첨자</th>

                            </tr>
                        </thead>
                        <tbody>
                            {draws.map(d => (
                                <tr key={d.id}>
                                    <td>{d.id}</td>
                                    <td>{d.drawDate}</td>
                                    <td>
                                        <span className="badge badge-primary mr-1">{d.num1}</span>
                                        <span className="badge badge-primary mr-1">{d.num2}</span>
                                        <span className="badge badge-primary mr-1">{d.num3}</span>
                                        <span className="badge badge-primary mr-1">{d.num4}</span>
                                        <span className="badge badge-primary mr-1">{d.num5}</span>
                                        <span className="badge badge-primary mr-1">{d.num6}</span>
                                    </td>
                                    <td><span className="badge badge-danger">{d.bonusNum}</span></td>
                                    <td>{d.totalPrize?.toLocaleString()}</td>
                                    <td>{d.winnerCount}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default DrawHistory;

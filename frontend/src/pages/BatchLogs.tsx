import React, { useEffect, useState } from 'react';
import { getBatchLogs } from '../services/api';

const BatchLogs: React.FC = () => {
    const [logs, setLogs] = useState<any[]>([]);

    useEffect(() => {
        loadLogs();
    }, []);

    const loadLogs = async () => {
        try {
            const res = await getBatchLogs(0, 50);
            setLogs(res.data.content);
        } catch (e) {
            console.error(e);
        }
    };

    return (
        <div>
            <div className="content-header">
                <h1>배치 로그</h1>
            </div>
            <div className="card">
                <div className="card-body p-0">
                    <table className="table table-striped">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>작업 유형</th>
                                <th>상태</th>
                                <th>메시지</th>
                                <th>실행일시</th>
                            </tr>
                        </thead>
                        <tbody>
                            {logs.map(log => (
                                <tr key={log.id}>
                                    <td>{log.id}</td>
                                    <td>{log.jobType}</td>
                                    <td>
                                        {/* NO_NEW: 새 회차가 아직 없음(정상) → 회색 */}
                                        <span className={`badge ${log.status === 'SUCCESS' ? 'badge-success' : log.status === 'NO_NEW' ? 'badge-secondary' : 'badge-danger'}`}>
                                            {log.status}
                                        </span>
                                    </td>
                                    <td>{log.message}</td>
                                    <td>{new Date(log.executedAt).toLocaleString()}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default BatchLogs;

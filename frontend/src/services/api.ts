import axios from 'axios';

const api = axios.create({
    // VITE_API_URL 환경 변수가 있으면 사용하고, 없으면 서버의 context-path(/lotto)를 포함한 경로 사용
    baseURL: import.meta.env.VITE_API_URL || '/lotto/api/v1',
});

// 쓰기(POST) API 관리자 토큰: 브라우저에 저장해 두고 헤더로 전송
const TOKEN_KEY = 'lottoAdminToken';
const readToken = () => { try { return localStorage.getItem(TOKEN_KEY); } catch { return null; } };

api.interceptors.request.use(config => {
    const token = readToken();
    if (token) config.headers['X-Admin-Token'] = token;
    return config;
});

// 401이면 토큰을 입력받아 저장한 뒤 한 번만 재시도, 재시도도 실패하면 잘못된 토큰을 지움
const retried = new WeakSet<object>();
api.interceptors.response.use(undefined, async error => {
    const config = error.config;
    if (error.response?.status !== 401 || !config) throw error;
    if (retried.has(config)) {
        try { localStorage.removeItem(TOKEN_KEY); } catch { /* 저장소 사용 불가 */ }
        throw error;
    }
    const input = window.prompt('관리자 토큰을 입력하세요');
    if (!input) throw error;
    try { localStorage.setItem(TOKEN_KEY, input); } catch { /* 저장 실패 시 이번 요청만 사용 */ }
    retried.add(config);
    config.headers['X-Admin-Token'] = input;
    return api(config);
});

export interface StrategyStat {
    strategy: string;
    total: number;
    avgMatch: number;
    wins: number;
}

export interface DashboardStats {
    latestDrawNo: number | null;
    latestDrawDate: string | null;
    latestNumbers: number[] | null;
    latestBonus: number | null;
    latestPrize: number | null;
    latestWinnerCount: number | null;
    totalDraws: number;
    numberFrequency: Record<string, number>;
    oddCount: number;
    evenCount: number;
    zoneDistribution: Record<string, number>;
    sumBins: Record<string, number>;
    lastDigitDistribution: Record<string, number>;
    latestDrawRecommendations: {
        targetDraw: number;
        numbers: number[];
        createdAt: string;
        strategy: string;
        matchCount: number;
        bonusMatch: boolean;
        resultLabel: string;
    }[];
    nextDrawRecommendations: {
        targetDraw: number;
        numbers: number[];
        createdAt: string;
        strategy: string;
        matchCount: number;
        bonusMatch: boolean;
        resultLabel: string;
    }[];
}

export const getDraws = (page = 0, size = 20) => api.get(`/lotto/draws?page=${page}&size=${size}`);
export const getDraw = (drawNo: number) => api.get(`/lotto/draws/${drawNo}`);
// count를 생략하면 서버 기본값(전략 수만큼) 사용
export const generateRecommendations = (count?: number) =>
    api.post('/lotto/recommend', null, { params: count ? { count } : {} });
export const syncDraw = (drawNo: number) => api.post(`/lotto/sync/${drawNo}`);
export const getBatchLogs = (page = 0, size = 20) => api.get(`/logs/batch?page=${page}&size=${size}`);
export const getDashboardStats = () => api.get<DashboardStats>('/stats/dashboard');
export const getStrategyStats = () => api.get<StrategyStat[]>('/stats/strategies');

export default api;

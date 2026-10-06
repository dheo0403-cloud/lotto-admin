import React from 'react';

interface LottoBallProps {
    number: number;
    size?: 'sm' | 'md' | 'lg';
    variant?: 'default' | 'match' | 'bonus' | 'solid' | 'dimmed';
}

const getBallColor = (num: number): string => {
    if (num <= 10) return '#fbc400'; // 노랑
    if (num <= 20) return '#69c8f2'; // 파랑
    if (num <= 30) return '#ff7272'; // 빨강
    if (num <= 40) return '#aaaaaa'; // 회색
    return '#b0d840'; // 초록
};

const LottoBall: React.FC<LottoBallProps> = ({ number, size = 'md', variant = 'default' }) => {
    const color = getBallColor(number);
    
    let border = `2px solid ${color}`;
    let backgroundColor = 'transparent';
    let textColor = color;
    let boxShadow = 'none';

    if (variant === 'match') {
        border = '3px solid #fbbf24'; // 금색 링
        boxShadow = '0 0 10px rgba(251, 191, 36, 0.4)';
    } else if (variant === 'bonus') {
        border = '3px solid #f1f5f9'; // 흰색 링
        boxShadow = '0 0 10px rgba(241, 245, 249, 0.4)';
    } else if (variant === 'solid') {
        backgroundColor = color;
        textColor = '#0f172a';
        boxShadow = `0 4px 8px ${color}40`;
    } else if (variant === 'dimmed') {
        border = `2px solid ${color}40`;
        textColor = `${color}80`;
    }

    return (
        <span
            className={`lotto-ball lotto-ball-${size}`}
            style={{
                border,
                backgroundColor,
                color: textColor,
                boxShadow,
            }}
        >
            {number}
        </span>
    );
};

export default LottoBall;

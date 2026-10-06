import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import Dashboard from './pages/Dashboard';
import DrawHistory from './pages/DrawHistory';
import Recommendation from './pages/Recommendation';
import BatchLogs from './pages/BatchLogs';

const App: React.FC = () => {
  return (
    <BrowserRouter basename="/lotto">
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="draws" element={<DrawHistory />} />
          <Route path="recommend" element={<Recommendation />} />
          <Route path="logs" element={<BatchLogs />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};

export default App;

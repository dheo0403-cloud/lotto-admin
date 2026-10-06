import React from 'react';
import { Outlet, Link } from 'react-router-dom';

const MainLayout: React.FC = () => {
  return (
    <>
      <nav className="main-header navbar navbar-expand navbar-white navbar-light">
        <ul className="navbar-nav">
          <li className="nav-item">
            <a className="nav-link" data-widget="pushmenu" href="#" role="button"><i className="fas fa-bars"></i></a>
          </li>
          <li className="nav-item d-none d-sm-inline-block">
            <Link to="/" className="nav-link">홈</Link>
          </li>
        </ul>
      </nav>

      <aside className="main-sidebar sidebar-dark-primary elevation-4">
        <Link to="/" className="brand-link">
          <span className="brand-text font-weight-light">로또 관리</span>
        </Link>
        <div className="sidebar">
          <nav className="mt-2">
            <ul className="nav nav-pills nav-sidebar flex-column" data-widget="treeview" role="menu" data-accordion="false">
              <li className="nav-item">
                <Link to="/" className="nav-link">
                  <i className="nav-icon fas fa-tachometer-alt"></i>
                  <p>대시보드</p>
                </Link>
              </li>
              <li className="nav-item">
                <Link to="/draws" className="nav-link">
                  <i className="nav-icon fas fa-list"></i>
                  <p>추첨 이력</p>
                </Link>
              </li>
              <li className="nav-item">
                <Link to="/recommend" className="nav-link">
                  <i className="nav-icon fas fa-magic"></i>
                  <p>번호 생성</p>
                </Link>
              </li>
              <li className="nav-item">
                <Link to="/logs" className="nav-link">
                  <i className="nav-icon fas fa-history"></i>
                  <p>배치 로그</p>
                </Link>
              </li>
            </ul>
          </nav>
        </div>
      </aside>

      <div className="content-wrapper">
        <section className="content pt-3">
          <div className="container-fluid">
            <Outlet />
          </div>
        </section>
      </div>

      <footer className="main-footer">
        <strong>Copyright &copy; 2026 AdminLotto.</strong>
      </footer>
    </>
  );
};

export default MainLayout;

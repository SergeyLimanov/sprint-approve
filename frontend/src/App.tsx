import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Teams from './pages/Teams';
import Users from './pages/Users';
import Sprints from './pages/Sprints';
import SprintDetail from './pages/SprintDetail';
import Tasks from './pages/Tasks';

function App() {
  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/teams" element={<Teams />} />
          <Route path="/users" element={<Users />} />
          <Route path="/sprints" element={<Sprints />} />
          <Route path="/sprints/:id" element={<SprintDetail />} />
          <Route path="/tasks" element={<Tasks />} />
        </Routes>
      </Layout>
    </Router>
  );
}

export default App;

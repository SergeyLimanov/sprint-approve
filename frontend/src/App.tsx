import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Teams from './pages/Teams';
import Users from './pages/Users';
import Sprints from './pages/Sprints';
import SprintDetail from './pages/SprintDetail';
import Tasks from './pages/Tasks';
import TaskDetail from './pages/TaskDetail';
import Notifications from './pages/Notifications';
import Login from './pages/Login';
import Register from './pages/Register';

// Компонент для защиты маршрутов
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem('accessToken');
  
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  
  return <>{children}</>;
}

function App() {
  return (
    <Router>
      <Routes>
        {/* Публичные маршруты */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        
        {/* Защищённые маршруты */}
        <Route
          path="/*"
          element={
            <ProtectedRoute>
              <Layout>
                <Routes>
                  <Route path="/" element={<Navigate to="/dashboard" replace />} />
                  <Route path="/dashboard" element={<Dashboard />} />
                  <Route path="/teams" element={<Teams />} />
                  <Route path="/users" element={<Users />} />
                  <Route path="/sprints" element={<Sprints />} />
                  <Route path="/sprints/:id" element={<SprintDetail />} />
                  <Route path="/tasks" element={<Tasks />} />
                  <Route path="/tasks/:id" element={<TaskDetail />} />
                  <Route path="/notifications" element={<Notifications />} />
                </Routes>
              </Layout>
            </ProtectedRoute>
          }
        />
      </Routes>
    </Router>
  );
}

export default App;

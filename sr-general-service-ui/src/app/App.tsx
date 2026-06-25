import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppLayout } from '../widgets/AppLayout/AppLayout';
import { AnalyticsPage } from '../pages/AnalyticsPage/AnalyticsPage';
import { AppealsPage } from '../pages/AppealsPage/AppealsPage';
import { AIAssistantPage } from '../pages/AIAssistantPage/AIAssistantPage';
import { LoginPage } from '../pages/LoginPage/LoginPage';
import { ProtectedRoute } from '../features/ProtectedRoute/ProtectedRoute';
import { UsersPage } from '../pages/UsersPage/UsersPage';
import { DangerousZonesPage } from '../pages/DangerousZonesPage/DangerousZonesPage';
import { RegionsPage } from '../pages/RegionsPage/RegionsPage';
import { RoadsPage } from '../pages/RoadsPage/RoadsPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/analytics" replace />} />
          <Route path="analytics" element={<AnalyticsPage />} />
          <Route path="appeals" element={<AppealsPage />} />
          <Route
            path="dangerous-zones"
            element={
              <ProtectedRoute allowedRoles={['MODERATOR']} redirectTo="/analytics">
                <DangerousZonesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="regions"
            element={
              <ProtectedRoute allowedRoles={['MODERATOR']} redirectTo="/analytics">
                <RegionsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="roads"
            element={
              <ProtectedRoute allowedRoles={['MODERATOR']} redirectTo="/analytics">
                <RoadsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="ai-assistant"
            element={
              <ProtectedRoute allowedRoles={['MODERATOR']} redirectTo="/analytics">
                <AIAssistantPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="users"
            element={
              <ProtectedRoute allowedRoles={['MODERATOR']} redirectTo="/analytics">
                <UsersPage />
              </ProtectedRoute>
            }
          />
        </Route>
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;

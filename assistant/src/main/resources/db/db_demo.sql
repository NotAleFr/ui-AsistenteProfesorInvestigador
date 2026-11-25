-- Datos de ejemplo para desarrollo. Se cargan solo al crear la BD con mvn exec:java (ver pom.xml).

INSERT INTO PlanAcademico (IdPlanAcademico, NombrePlan, AnioPlan, PeriodoPlan, Activo) VALUES
  (1, 'Plan 2026-2', 2026, '2026-2', 1),
  (2, 'Plan 2026-1', 2026, '2026-1', 0);

INSERT INTO Evidencia (Titulo, Descripcion, Anio, Periodo, FechaInicio, FechaFinalizacion, IdTipoEvidencia, IdOpcion, IdEstatus, IdAcreditacion, IdPlanAcademico) VALUES
  ('Rediseño del plan de Ingeniería en Sistemas', 'Comisión del nuevo plan de estudios', 2026, '2026-2', '2026-08-15', '2026-12-31', 1, 1, 2, 1, 1),
  ('Tutorías de licenciatura', 'Grupo de tutorados de primer semestre', 2026, '2026-2', '2026-08-15', '2026-12-31', 54, 169, 3, 2, 1),
  ('Renovación SNII', NULL, 2026, '2026-2', NULL, NULL, 33, NULL, 1, 1, 1),
  ('Comisión dictaminadora del departamento', NULL, 2026, '2026-2', '2026-08-15', '2026-12-31', 70, 212, 1, 1, 1),
  ('Constancia de curso de actualización', 'Evidencia fuera del catálogo', 2026, '2026-2', NULL, NULL, 82, NULL, 1, 1, 1);

-- El plan 2 es borrador: sus registros no salen en Seguimiento
INSERT INTO Evidencia (Titulo, Descripcion, Anio, Periodo, IdTipoEvidencia, IdPlanAcademico) VALUES
  ('Responsable de tutorías del programa', NULL, 2026, '2026-1', 53, 2),
  ('Reestructuración del plan de Licenciatura', NULL, 2026, '2026-1', 2, 2);

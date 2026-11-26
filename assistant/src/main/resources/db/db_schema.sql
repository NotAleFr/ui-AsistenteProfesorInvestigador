-- ============================================================
-- Creación de la base de datos de evidencias (SQLite 3.37+)
-- ============================================================
PRAGMA foreign_keys = ON;

-- ---------- Catálogos ----------

CREATE TABLE Categoria (
    IdCategoria         INTEGER PRIMARY KEY AUTOINCREMENT,
    ClaveCategoria      TEXT NOT NULL UNIQUE,
    NombreCategoria     TEXT NOT NULL,
    DefinicionCategoria TEXT
) STRICT;

CREATE TABLE TipoEvidencia (
    IdTipoEvidencia         INTEGER PRIMARY KEY AUTOINCREMENT,
    ClaveTipoEvidencia      TEXT NOT NULL UNIQUE,
    NombreTipoEvidencia     TEXT NOT NULL,
    DefinicionTipoEvidencia TEXT,
    ContenidoAcreditacion   TEXT,
    PuntajeMaximo           REAL CHECK (PuntajeMaximo >= 0),
    IdCategoria             INTEGER NOT NULL,
    FOREIGN KEY (IdCategoria) REFERENCES Categoria (IdCategoria)
        ON UPDATE CASCADE ON DELETE RESTRICT
) STRICT;

CREATE TABLE Opcion (
    IdOpcion        INTEGER PRIMARY KEY AUTOINCREMENT,
    NombreOpcion    TEXT NOT NULL,
    PuntajeOpcion   REAL CHECK (PuntajeOpcion >= 0),
    OrdenOpcion     INTEGER NOT NULL,
    IdTipoEvidencia INTEGER NOT NULL,
    UNIQUE (IdTipoEvidencia, OrdenOpcion),
    UNIQUE (IdOpcion, IdTipoEvidencia),
    FOREIGN KEY (IdTipoEvidencia) REFERENCES TipoEvidencia (IdTipoEvidencia)
        ON UPDATE CASCADE ON DELETE RESTRICT
) STRICT;

CREATE TABLE Estatus (
    IdEstatus     INTEGER PRIMARY KEY AUTOINCREMENT,
    NombreEstatus TEXT NOT NULL UNIQUE
) STRICT;

CREATE TABLE Acreditacion (
    IdAcreditacion     INTEGER PRIMARY KEY AUTOINCREMENT,
    NombreAcreditacion TEXT NOT NULL UNIQUE
) STRICT;

-- ---------- Tablas principales ----------

CREATE TABLE Evidencia (
    IdEvidencia       INTEGER PRIMARY KEY AUTOINCREMENT,
    Titulo            TEXT NOT NULL,
    Descripcion       TEXT,
    Anio              INTEGER NOT NULL CHECK (Anio BETWEEN 1900 AND 2200),
    Periodo           TEXT NOT NULL,
    FechaInicio       TEXT,                -- ISO 8601: YYYY-MM-DD
    FechaFinalizacion TEXT,                -- ISO 8601: YYYY-MM-DD
    FechaModificacion TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    IdTipoEvidencia   INTEGER NOT NULL,
    IdOpcion          INTEGER,             -- se elige en Seguimiento; NULL mientras no se elija
    IdEstatus         INTEGER NOT NULL DEFAULT 1,   -- 1 = Pendiente
    IdAcreditacion    INTEGER NOT NULL DEFAULT 1,   -- 1 = Pendiente
    IdPlanAcademico   INTEGER NOT NULL,
    CHECK (FechaInicio IS NULL OR FechaFinalizacion IS NULL
           OR FechaFinalizacion >= FechaInicio),
    FOREIGN KEY (IdTipoEvidencia) REFERENCES TipoEvidencia (IdTipoEvidencia)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    -- La opción debe pertenecer al mismo tipo de actividad de la evidencia
    FOREIGN KEY (IdOpcion, IdTipoEvidencia) REFERENCES Opcion (IdOpcion, IdTipoEvidencia)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (IdEstatus)       REFERENCES Estatus (IdEstatus)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (IdAcreditacion)  REFERENCES Acreditacion (IdAcreditacion)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (IdPlanAcademico) REFERENCES PlanAcademico (IdPlanAcademico)
        ON UPDATE CASCADE ON DELETE RESTRICT
) STRICT;

CREATE TABLE Documento (
    IdDocumento        INTEGER PRIMARY KEY AUTOINCREMENT,
    RutaAlmacenamiento TEXT NOT NULL UNIQUE,
    NombreOriginal     TEXT NOT NULL,
    TamanoBytes        INTEGER NOT NULL CHECK (TamanoBytes >= 0),
    SHA256             TEXT NOT NULL CHECK (length(SHA256) = 64),
    FechaSubida        TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    IdEvidencia        INTEGER NOT NULL,
    FOREIGN KEY (IdEvidencia) REFERENCES Evidencia (IdEvidencia)
        ON UPDATE CASCADE ON DELETE CASCADE
) STRICT;

CREATE TABLE PlanAcademico (
    IdPlanAcademico INTEGER PRIMARY KEY AUTOINCREMENT,
    NombrePlan      TEXT NOT NULL,
    AnioPlan        INTEGER NOT NULL,
    PeriodoPlan     TEXT NOT NULL,
    Activo          INTEGER NOT NULL DEFAULT 0 CHECK (Activo IN (0, 1))  -- 0 = borrador (RF-07), 1 = en Seguimiento (RF-08)
) STRICT;

CREATE TABLE Alertas (
	IdAlerta INTEGER PRIMARY KEY AUTOINCREMENT,
	ContenidoAlerta TEXT NOT NULL,
	FechaHoraCreacion TEXT DEFAULT CURRENT_TIMESTAMP
) STRICT;

-- ---------- Índices sobre llaves foráneas ----------

CREATE INDEX idx_tipoevidencia_categoria ON TipoEvidencia (IdCategoria);
CREATE INDEX idx_evidencia_tipo          ON Evidencia (IdTipoEvidencia);
CREATE INDEX idx_evidencia_opcion        ON Evidencia (IdOpcion);
CREATE INDEX idx_evidencia_estatus       ON Evidencia (IdEstatus);
CREATE INDEX idx_evidencia_acreditacion  ON Evidencia (IdAcreditacion);
CREATE INDEX idx_evidencia_plan          ON Evidencia (IdPlanAcademico);
CREATE INDEX idx_documento_evidencia     ON Documento (IdEvidencia);

-- ---------- Trigger: actualizar FechaModificacion ----------

CREATE TRIGGER trg_evidencia_fecha_modificacion
AFTER UPDATE ON Evidencia
FOR EACH ROW
WHEN NEW.FechaModificacion = OLD.FechaModificacion
BEGIN
    UPDATE Evidencia
       SET FechaModificacion = CURRENT_TIMESTAMP
     WHERE IdEvidencia = NEW.IdEvidencia;
END;

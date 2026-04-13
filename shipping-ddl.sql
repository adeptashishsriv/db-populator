
CREATE SCHEMA shipping;

CREATE TABLE shipping.Customers (
    CustomerID       INT IDENTITY(1,1) PRIMARY KEY,
    CustomerName     NVARCHAR(200) NOT NULL,
    Email            NVARCHAR(200),
    Phone            NVARCHAR(50),
    AddressLine1     NVARCHAR(200),
    City             NVARCHAR(100),
    Country          NVARCHAR(100),
    CreatedAt        DATETIME2 DEFAULT SYSUTCDATETIME()
);

CREATE TABLE shipping.Employees (
    EmployeeID       INT IDENTITY(1,1) PRIMARY KEY,
    FullName         NVARCHAR(200) NOT NULL,
    Role             NVARCHAR(50) NOT NULL,  -- Driver, Dispatcher
    Phone            NVARCHAR(50),
    HireDate         DATE NOT NULL
);
CREATE TABLE shipping.Vehicles (
    VehicleID        INT IDENTITY(1,1) PRIMARY KEY,
    VehicleNumber    NVARCHAR(50) NOT NULL,
    Type             NVARCHAR(50),          -- Van, Truck, Bike
    CapacityKg       INT,
    Active           BIT DEFAULT 1
);

CREATE TABLE shipping.Shipments (
    ShipmentID       INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID       INT NOT NULL,
    OriginCity       NVARCHAR(100),
    DestinationCity  NVARCHAR(100),
    Status           NVARCHAR(50) DEFAULT 'Created', -- Created, In Transit, Delivered
    CreatedAt        DATETIME2 DEFAULT SYSUTCDATETIME(),
    AssignedDriverID INT NULL,
    VehicleID        INT NULL,

    CONSTRAINT FK_Shipments_Customers FOREIGN KEY (CustomerID)
        REFERENCES shipping.Customers(CustomerID),

    CONSTRAINT FK_Shipments_Employees FOREIGN KEY (AssignedDriverID)
        REFERENCES shipping.Employees(EmployeeID),

    CONSTRAINT FK_Shipments_Vehicles FOREIGN KEY (VehicleID)
        REFERENCES shipping.Vehicles(VehicleID)
);

CREATE TABLE shipping.Packages (
    PackageID        INT IDENTITY(1,1) PRIMARY KEY,
    ShipmentID       INT NOT NULL,
    WeightKg         DECIMAL(10,2),
    Description      NVARCHAR(200),
    CONSTRAINT FK_Packages_Shipments FOREIGN KEY (ShipmentID)
        REFERENCES shipping.Shipments(ShipmentID)
);

CREATE TABLE shipping.DeliveryEvents (
    EventID          INT IDENTITY(1,1) PRIMARY KEY,
    ShipmentID       INT NOT NULL,
    EventTime        DATETIME2 DEFAULT SYSUTCDATETIME(),
    Location         NVARCHAR(200),
    EventDescription NVARCHAR(200),

    CONSTRAINT FK_DeliveryEvents_Shipments FOREIGN KEY (ShipmentID)
        REFERENCES shipping.Shipments(ShipmentID)
);

CREATE VIEW shipping.vw_ShipmentSummary
AS
SELECT 
    s.ShipmentID,
    c.CustomerName,
    s.OriginCity,
    s.DestinationCity,
    s.Status,
    e.FullName AS DriverName,
    v.VehicleNumber,
    s.CreatedAt
FROM shipping.Shipments s
LEFT JOIN shipping.Customers c ON s.CustomerID = c.CustomerID
LEFT JOIN shipping.Employees e ON s.AssignedDriverID = e.EmployeeID
LEFT JOIN shipping.Vehicles v ON s.VehicleID = v.VehicleID;

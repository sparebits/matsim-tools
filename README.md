# MATSim Tools

Matsim Tools is a project designed to work with MATSim (Multi-Agent Transport Simulation) data. It includes tools 
for processing, visualizing, and analyzing MATSim network and population files. 

## Features

- **Integration with OpenStreetMap**: Support for MATSim networks based on OpenStreetMap data. Generate MATSim 
  network data based on the bounded area.
- **Population Service**: Generate and manipulate population plans for MATSim simulations.
- **Network Visualization**: Visualize MATSim network files for better understanding and debugging.

## Project Structure

- **matsim-service**: Backend services for processing MATSim data.
- **matsim-ui**: Frontend application for visualizing MATSim networks.

## Development Notes

### Backend (matsim-service)

1. **Build the project**:
   ```bash
   mvn clean install
   ```

2. **Run the service**:
   ```bash
   mvn spring-boot:run
   ```

3. **Endpoints**:
   - `POST /network`: Upload a MATSim network file.
   - `POST /plan`: Generate a plan for a given origin and destination.
   - `GET /plan`: Download the generated population in XML format.
   - `GET /plan/random`: Generate random population
   - `GET /osm/nodes`: Generate MATSim network model based on the focused area

### Frontend (matsim-ui)

1. **Install dependencies**:
   ```bash
   npm install
   ```

2. **Run the development server**:
   ```bash
   ng serve
   ```

   Open your browser and navigate to `http://localhost:4200/`.

3. **Build the project**:
   ```bash
   ng build
   ```

   The build artifacts will be stored in the `dist/` directory.

## Related Projects

Here are some repositories that complement or are related to this project:

- [MATSim](https://github.com/matsim-org/matsim-libs): The official MATSim library for multi-agent transport simulation.
- [OpenStreetMap](https://github.com/openstreetmap/openstreetmap-website): The OpenStreetMap project for geographic data.
- [MATSim Examples](https://github.com/matsim-org/matsim-code-examples): Example projects and use cases for MATSim.

## References

- [MATSim Documentation](https://www.matsim.org/documentation): Official documentation for MATSim.
- [Spring Boot](https://spring.io/projects/spring-boot): Framework used for the backend service.
- [Angular](https://angular.io/): Framework used for the frontend application.

## License

This project is licensed under the LGPL 2.1 License. See the [`LICENSE`](LICENSE) file for details.


package org.geoshell.vector

import geoscript.layer.Layer
import geoscript.layer.Shapefile
import geoscript.workspace.Directory
import geoscript.workspace.GeoPackage
import geoscript.workspace.Memory
import geoscript.workspace.Workspace
import org.geoshell.Catalog
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.shell.support.util.OsUtils
import static org.junit.Assert.*

class LayerCommandsTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder()

    @Test void open() {
        Catalog catalog = new Catalog()
        Workspace workspace = new Memory()
        workspace.add(new Layer("points"))
        workspace.add(new Layer("lines"))
        workspace.add(new Layer("polygons"))
        catalog.workspaces[new WorkspaceName("shapes")] = workspace

        LayerCommands cmds = new LayerCommands(catalog: catalog)
        String result = cmds.open(new WorkspaceName("shapes"), new LayerName("points"), null)
        assertEquals "Opened Workspace shapes Layer points as shapes:points", result
        assertNotNull catalog.layers[new LayerName("shapes:points")]

        result = cmds.open(new WorkspaceName("shapes"), new LayerName("lines"), "lines")
        assertEquals "Opened Workspace shapes Layer lines as lines", result
        assertNotNull catalog.layers[new LayerName("lines")]
    }

    @Test void close() {
        Catalog catalog = new Catalog()
        Workspace workspace = new Memory()
        workspace.add(new Layer("points"))
        workspace.add(new Layer("lines"))
        workspace.add(new Layer("polygons"))
        catalog.workspaces[new WorkspaceName("shapes")] = workspace

        LayerCommands cmds = new LayerCommands(catalog: catalog)
        // Open
        cmds.open(new WorkspaceName("shapes"), new LayerName("points"), "")
        assertNotNull catalog.layers[new LayerName("shapes:points")]
        // Close
        String result = cmds.close(new LayerName("shapes:points"))
        assertEquals "Layer shapes:points closed!", result
        assertNull catalog.layers[new LayerName("shapes:points")]
    }

    @Test void list() {
        Catalog catalog = new Catalog()
        Workspace workspace = new Memory()
        workspace.add(new Layer("points"))
        workspace.add(new Layer("lines"))
        workspace.add(new Layer("polygons"))
        catalog.workspaces[new WorkspaceName("shapes")] = workspace

        LayerCommands cmds = new LayerCommands(catalog: catalog)
        cmds.open(new WorkspaceName("shapes"), new LayerName("points"), null)
        cmds.open(new WorkspaceName("shapes"), new LayerName("lines"), "lines")
        String result = cmds.list()
        assertEquals "shapes:points = Memory" + OsUtils.LINE_SEPARATOR + "lines = Memory", result
    }

    @Test void remove() {
        Catalog catalog = new Catalog()
        Workspace workspace = new Memory()
        workspace.add(new Layer("points"))
        workspace.add(new Layer("lines"))
        workspace.add(new Layer("polygons"))
        catalog.workspaces[new WorkspaceName("shapes")] = workspace

        LayerCommands cmds = new LayerCommands(catalog: catalog)
        String result = cmds.remove(new WorkspaceName("shapes"), new LayerName("points"))
        assertEquals "Layer points removed from Workspace shapes", result
        assertFalse workspace.has("points")
        assertTrue workspace.has("lines")
        assertTrue workspace.has("polygons")
    }

    @Test void count() {
        Layer layer = new Shapefile(new File(getClass().getClassLoader().getResource("points.shp").toURI()))
        Catalog catalog = new Catalog()
        catalog.layers[new LayerName("points")] = layer
        LayerCommands cmds = new LayerCommands(catalog: catalog)
        String result = cmds.count(new LayerName("points"))
        assertEquals "10", result
    }

    @Test
    void schema() {
        Catalog catalog = new Catalog()
        File file = new File(getClass().getClassLoader().getResource("points.shp").toURI())
        catalog.workspaces[new WorkspaceName("shps")] = new Directory(file.parentFile)
        catalog.layers[new LayerName("points")] = catalog.workspaces[new WorkspaceName("shps")].get("points")

        LayerCommands cmds = new LayerCommands(catalog: catalog)
        String actual = cmds.schema(new LayerName("points"))
        String expected = "  Name                  Type" + OsUtils.LINE_SEPARATOR +
                "  --------------------  --------------------" + OsUtils.LINE_SEPARATOR +
                "  the_geom              Point" + OsUtils.LINE_SEPARATOR +
                "  id                    Integer" + OsUtils.LINE_SEPARATOR
        assertEquals expected, actual
    }

    @Test void buffer() {
        Layer layer = new Shapefile(new File(getClass().getClassLoader().getResource("points.shp").toURI()))
        Catalog catalog = new Catalog()
        catalog.layers[new LayerName("points")] = layer
        catalog.workspaces[new WorkspaceName("mem")] = new Memory()
        LayerCommands cmds = new LayerCommands(catalog: catalog)

        String result = cmds.buffer(new LayerName("points"), new WorkspaceName("mem"), "polys", 10)
        assertEquals "Done!", result

        assertNotNull catalog.layers[new LayerName("polys")]
        Layer bufferedLayer = catalog.layers[new LayerName("polys")]
        assertEquals layer.count, bufferedLayer.count
        assertEquals "Polygon", bufferedLayer.schema.geom.typ
    }

    @Test void centroid() {
        Layer layer = new Shapefile(new File(getClass().getClassLoader().getResource("grid.shp").toURI()))
        Catalog catalog = new Catalog()
        catalog.layers[new LayerName("grid")] = layer
        catalog.workspaces[new WorkspaceName("mem")] = new Memory()
        LayerCommands cmds = new LayerCommands(catalog: catalog)

        String result = cmds.centroids(new LayerName("grid"), new WorkspaceName("mem"), "centroids")
        assertEquals "Done!", result

        assertNotNull catalog.layers[new LayerName("centroids")]
        Layer centroidLayer = catalog.layers[new LayerName("centroids")]
        assertEquals layer.count, centroidLayer.count
        assertEquals "Point", centroidLayer.schema.geom.typ
    }

    @Test void interiorPoint() {
        Layer layer = new Shapefile(new File(getClass().getClassLoader().getResource("grid.shp").toURI()))
        Catalog catalog = new Catalog()
        catalog.layers[new LayerName("grid")] = layer
        catalog.workspaces[new WorkspaceName("mem")] = new Memory()
        LayerCommands cmds = new LayerCommands(catalog: catalog)

        String result = cmds.interiorPoints(new LayerName("grid"), new WorkspaceName("mem"), "interiorPoints")
        assertEquals "Done!", result

        assertNotNull catalog.layers[new LayerName("interiorPoints")]
        Layer centroidLayer = catalog.layers[new LayerName("interiorPoints")]
        assertEquals layer.count, centroidLayer.count
        assertEquals "Point", centroidLayer.schema.geom.typ
    }

    @Test void random() {
        Catalog catalog = new Catalog()
        catalog.workspaces[new WorkspaceName("mem")] = new Memory()
        LayerCommands cmds = new LayerCommands(catalog: catalog)

        String result = cmds.random(new WorkspaceName("mem"), "points", 100, "0,0,100,100", "EPSG:4326", "id", "geom", false, false, 0)
        assertEquals "Done!", result

        assertNotNull catalog.layers[new LayerName("points")]
        Layer layer = catalog.layers[new LayerName("points")]
        assertEquals 100, layer.count
        assertEquals "Point", layer.schema.geom.typ
    }

    @Test void gridRowColumn() {
        Catalog catalog = new Catalog()
        catalog.workspaces[new WorkspaceName("mem")] = new Memory()
        LayerCommands cmds = new LayerCommands(catalog: catalog)

        String result = cmds.gridRowColumn(
                new WorkspaceName("mem"),
                "grid",
                10, 10,
                "-180,-90,180,90",
                "polygon",
                "EPSG:4326",
                "geom"
        )
        assertEquals "Done!", result

        assertNotNull catalog.layers[new LayerName("grid")]
        Layer layer = catalog.layers[new LayerName("grid")]
        assertEquals 100, layer.count
        assertEquals "Polygon", layer.schema.geom.typ
    }

    @Test void gridWidthHeight() {
        Catalog catalog = new Catalog()
        catalog.workspaces[new WorkspaceName("mem")] = new Memory()
        LayerCommands cmds = new LayerCommands(catalog: catalog)

        String result = cmds.gridWidthHeight(
                new WorkspaceName("mem"),
                "grid",
                20, 20,
                "-180,-90,180,90",
                "polygon",
                "EPSG:4326",
                "geom"
        )
        assertEquals "Done!", result

        assertNotNull catalog.layers[new LayerName("grid")]
        Layer layer = catalog.layers[new LayerName("grid")]
        assertEquals 162, layer.count
        assertEquals "Polygon", layer.schema.geom.typ
    }

    @Test void getSetStyle() {
        Catalog catalog = new Catalog()
        Workspace workspace = new Memory()
        workspace.add(new Layer("points"))
        workspace.add(new Layer("lines"))
        workspace.add(new Layer("polygons"))
        catalog.workspaces[new WorkspaceName("shapes")] = workspace
        LayerCommands cmds = new LayerCommands(catalog: catalog)
        cmds.open(new WorkspaceName("shapes"), new LayerName("points"), "points")

        File sldFile = folder.newFile("points.sld")

        String result = cmds.getStyle(new LayerName("points"), sldFile)
        assertTrue result.startsWith("points style written to")
        assertTrue result.endsWith("points.sld")

        result = cmds.getStyle(new LayerName("points"), null)
        assertTrue result.contains("<sld:StyledLayerDescriptor")

        result = cmds.setStyle(new LayerName("points"), sldFile)
        assertTrue result.startsWith("Style ")
        assertTrue result.endsWith("points.sld set on points")
    }

    @Test void copy() {
        Catalog catalog = new Catalog()
        catalog.workspaces[new WorkspaceName("mem")] = new Memory()
        catalog.workspaces[new WorkspaceName("gpkg")] = new GeoPackage(folder.newFile("layers.gpkg"))
        LayerCommands cmds = new LayerCommands(catalog: catalog)

        // Create 100 random points
        String result = cmds.random(new WorkspaceName("mem"), "points", 100, "0,0,100,100", "EPSG:4326", "the_id", "geom", false, false, 0)
        assertEquals "Done!", result

        assertNotNull catalog.layers[new LayerName("points")]
        Layer layer = catalog.layers[new LayerName("points")]
        assertEquals 100, layer.count
        assertEquals "Point", layer.schema.geom.typ

        // Copy all points
        result = cmds.copy(new LayerName("points"), new WorkspaceName("gpkg"), "geopoints1", null, null, -1, -1, null)
        assertEquals "Done!", result

        assertNotNull catalog.layers[new LayerName("geopoints1")]
        layer = catalog.layers[new LayerName("geopoints1")]
        assertEquals 100, layer.count
        assertEquals "Point", layer.schema.geom.typ

        // Copy all points sorted by id
        result = cmds.copy(new LayerName("points"), new WorkspaceName("gpkg"), "geopoints2", null, "the_id desc", -1, -1, null)
        assertEquals "Done!", result

        assertNotNull catalog.layers[new LayerName("geopoints2")]
        layer = catalog.layers[new LayerName("geopoints2")]
        assertEquals 100, layer.count
        assertEquals "Point", layer.schema.geom.typ

        // Copy some points (filter)
        result = cmds.copy(new LayerName("points"), new WorkspaceName("gpkg"), "geopoints3", "the_id > 49", null, -1, -1, null)
        assertEquals "Done!", result

        assertNotNull catalog.layers[new LayerName("geopoints3")]
        layer = catalog.layers[new LayerName("geopoints3")]
        assertEquals 50, layer.count
        assertEquals "Point", layer.schema.geom.typ

        // Copy some points (start)
        result = cmds.copy(new LayerName("points"), new WorkspaceName("gpkg"), "geopoints4", null, null, 80, 100, null)
        assertEquals "Done!", result

        assertNotNull catalog.layers[new LayerName("geopoints4")]
        layer = catalog.layers[new LayerName("geopoints4")]
        assertEquals 20, layer.count
        assertEquals "Point", layer.schema.geom.typ

        // Copy some points (start and max)
        result = cmds.copy(new LayerName("points"), new WorkspaceName("gpkg"), "geopoints5", null, null, 80, 10, null)
        assertEquals "Done!", result

        assertNotNull catalog.layers[new LayerName("geopoints5")]
        layer = catalog.layers[new LayerName("geopoints5")]
        assertEquals 10, layer.count
        assertEquals "Point", layer.schema.geom.typ

        // Copy some points (max)
        result = cmds.copy(new LayerName("points"), new WorkspaceName("gpkg"), "geopoints6", null, null, 0, 10, null)
        assertEquals "Done!", result

        assertNotNull catalog.layers[new LayerName("geopoints6")]
        layer = catalog.layers[new LayerName("geopoints6")]
        assertEquals 10, layer.count
        assertEquals "Point", layer.schema.geom.typ

        // Copy all but only one field
        result = cmds.copy(new LayerName("points"), new WorkspaceName("gpkg"), "geopoints7", null, null, -1, -1, "geom")
        assertEquals "Done!", result

        assertNotNull catalog.layers[new LayerName("geopoints7")]
        layer = catalog.layers[new LayerName("geopoints7")]
        assertEquals 100, layer.count
        assertEquals "Point", layer.schema.geom.typ
        assertTrue layer.schema.has("geom")
        assertFalse layer.schema.has("the_id")
    }

    @Test void extent() {
        Layer layer = new Shapefile(new File(getClass().getClassLoader().getResource("points.shp").toURI()))
        Catalog catalog = new Catalog()
        catalog.workspaces[new WorkspaceName("mem")] = new Memory()
        catalog.layers[new LayerName("points")] = layer
        LayerCommands cmds = new LayerCommands(catalog: catalog)
        String result = cmds.extent(new LayerName("points"), new WorkspaceName("mem"), "extent", "geom")
        assertEquals "Done!", result
        assertNotNull catalog.layers[new LayerName("extent")]
        layer = catalog.layers[new LayerName("extent")]
        assertEquals 1, layer.count
        assertEquals "Polygon", layer.schema.geom.typ
    }

    @Test void extents() {
        Layer layer = new Shapefile(new File(getClass().getClassLoader().getResource("points.shp").toURI()))
        Catalog catalog = new Catalog()
        catalog.workspaces[new WorkspaceName("mem")] = new Memory()
        catalog.layers[new LayerName("points")] = layer
        LayerCommands cmds = new LayerCommands(catalog: catalog)
        cmds.buffer(new LayerName("points"), new WorkspaceName("mem"), "buffer", 10)
        String result = cmds.extents(new LayerName("buffer"), new WorkspaceName("mem"), "extents")
        assertEquals "Done!", result
        assertNotNull catalog.layers[new LayerName("extents")]
        Layer outLayer = catalog.layers[new LayerName("extents")]
        assertEquals layer.count, outLayer.count
        assertEquals "Polygon", outLayer.schema.geom.typ
    }

    @Test void convexhull() {
        Layer layer = new Shapefile(new File(getClass().getClassLoader().getResource("points.shp").toURI()))
        Catalog catalog = new Catalog()
        catalog.workspaces[new WorkspaceName("mem")] = new Memory()
        catalog.layers[new LayerName("points")] = layer
        LayerCommands cmds = new LayerCommands(catalog: catalog)
        String result = cmds.convexhull(new LayerName("points"), new WorkspaceName("mem"), "convexhull", "geom")
        assertEquals "Done!", result
        assertNotNull catalog.layers[new LayerName("convexhull")]
        layer = catalog.layers[new LayerName("convexhull")]
        assertEquals 1, layer.count
        assertEquals "Polygon", layer.schema.geom.typ
    }

    @Test void convexhulls() {
        Layer layer = new Shapefile(new File(getClass().getClassLoader().getResource("points.shp").toURI()))
        Catalog catalog = new Catalog()
        catalog.workspaces[new WorkspaceName("mem")] = new Memory()
        catalog.layers[new LayerName("points")] = layer
        LayerCommands cmds = new LayerCommands(catalog: catalog)
        cmds.buffer(new LayerName("points"), new WorkspaceName("mem"), "buffer", 10)
        String result = cmds.convexhulls(new LayerName("buffer"), new WorkspaceName("mem"), "convexhulls")
        assertEquals "Done!", result
        assertNotNull catalog.layers[new LayerName("convexhulls")]
        Layer outLayer = catalog.layers[new LayerName("convexhulls")]
        assertEquals layer.count, outLayer.count
        assertEquals "Polygon", outLayer.schema.geom.typ
    }

    @Test void voronoi() {
        Layer layer = new Shapefile(new File(getClass().getClassLoader().getResource("points.shp").toURI()))
        Catalog catalog = new Catalog()
        catalog.workspaces[new WorkspaceName("mem")] = new Memory()
        catalog.layers[new LayerName("points")] = layer
        LayerCommands cmds = new LayerCommands(catalog: catalog)
        String result = cmds.voronoi(new LayerName("points"), new WorkspaceName("mem"), "voronoi", "geom")
        assertEquals "Done!", result
        assertNotNull catalog.layers[new LayerName("voronoi")]
        layer = catalog.layers[new LayerName("voronoi")]
        assertEquals 10, layer.count
        assertEquals "Polygon", layer.schema.geom.typ
    }
}

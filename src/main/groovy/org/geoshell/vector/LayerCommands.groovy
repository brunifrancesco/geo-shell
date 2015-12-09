package org.geoshell.vector

import geoscript.feature.Feature
import geoscript.feature.Field
import geoscript.feature.Schema
import geoscript.geom.Geometry
import geoscript.geom.MultiPoint
import geoscript.geom.Point
import geoscript.layer.Layer
import geoscript.layer.Writer as LayerWriter
import geoscript.style.Style
import geoscript.style.io.CSSReader
import geoscript.style.io.SLDReader
import geoscript.style.io.SLDWriter
import geoscript.workspace.Workspace
import org.geoshell.Catalog
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.shell.core.CommandMarker
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption
import org.springframework.shell.support.table.Table
import org.springframework.shell.support.table.TableHeader
import org.springframework.shell.support.table.TableRow
import org.springframework.shell.support.util.OsUtils
import org.springframework.stereotype.Component

@Component
class LayerCommands implements CommandMarker {

    @Autowired
    Catalog catalog

    @CliCommand(value = "layer open", help = "Open a Layer.")
    String open(
            @CliOption(key = "workspace", mandatory = true, help = "The Workspace name") WorkspaceName workspaceName,
            @CliOption(key = "layer", mandatory = true, help = "The Layer name") LayerName layerName,
            @CliOption(key = "name", mandatory = false, help = "The name") String name
    ) throws Exception {
        if (!name) {
            name = "${workspaceName.name}:${layerName.name}"
        }
        Workspace workspace = catalog.workspaces[workspaceName]
        if (workspace) {
            if (workspace.has(layerName.name)) {
                Layer layer = workspace.get(layerName.name)
                catalog.layers[new LayerName(name)] = layer
                "Opened Workspace ${workspaceName.name} Layer ${layerName.name} as ${name}"
            } else {
                "Unable to find Layer ${layerName}"
            }
        } else {
            "Unable to find Workspace ${workspaceName}"
        }
    }

    @CliCommand(value = "layer close", help = "Close a Layer.")
    String close(
            @CliOption(key = "name", mandatory = true, help = "The Layer name") LayerName name
    ) throws Exception {
        Layer layer = catalog.layers[name]
        if (layer) {
            catalog.layers.remove(name)
            "Layer ${name} closed!"
        } else {
            "Unable to find Layer ${name}"
        }
    }

    @CliCommand(value = "layer list", help = "List open Layers.")
    String list() throws Exception {
        catalog.layers.collect { LayerName name, Layer layer ->
            "${name} = ${layer.workspace.format}"
        }.join(OsUtils.LINE_SEPARATOR)
    }

    @CliCommand(value = "layer count", help = "Count the Feature in a Layer.")
    String count(
            @CliOption(key = "name", mandatory = true, help = "The Layer name") LayerName name
    ) throws Exception {
        Layer layer = catalog.layers[name]
        if (layer) {
            "${layer.count}"
        } else {
            "Unable to find Layer ${name}"
        }
    }

    @CliCommand(value = "layer schema", help = "Inspect a Layer's Schema.")
    String schema(
            @CliOption(key = "name", mandatory = true, help = "The Layer name") LayerName name
    ) throws Exception {
        Layer layer = catalog.layers[name]
        if (layer) {
            Table table = new Table()
            table.addHeader(0, new TableHeader("Name", 20))
            table.addHeader(1, new TableHeader("Type", 20))
            layer.schema.fields.each { Field f ->
                TableRow row = table.newRow()
                row.addValue(0, f.name)
                row.addValue(1, f.typ)
            }
            table.calculateColumnWidths()
            table.toString()
        } else {
            "Unable to find Layer ${name}"
        }
    }

    @CliCommand(value = "layer style set", help = "Set a Layer's style")
    String setStyle(
            @CliOption(key = "name", mandatory = true, help = "The Layer name") LayerName name,
            @CliOption(key = "style", mandatory = true, help = "The SLD or CSS File") File styleFile
    ) throws Exception {
        Layer layer = catalog.layers[name]
        if (layer) {
            Style style = null
            if (styleFile.name.endsWith(".sld")) {
                style = new SLDReader().read(styleFile)
            } else if (styleFile.name.endsWith(".css")) {
                style = new CSSReader().read(styleFile)
            }
            if (style) {
                layer.style = style
                "Style ${styleFile.absolutePath} set on ${name}"
            } else {
                "Unable to read ${styleFile.absolutePath}"
            }
        } else {
            "Unable to find Layer ${name}"
        }
    }

    @CliCommand(value = "layer style get", help = "Get the Layer's style.")
    String getStyle(
            @CliOption(key = "name", mandatory = true, help = "The Layer name") LayerName name,
            @CliOption(key = "style", mandatory = false, help = "The SLD File") File styleFile
    ) throws Exception {
        Layer layer = catalog.layers[name]
        if (layer) {
            if (styleFile) {
                new SLDWriter().write(layer.style, styleFile)
                "${name} style written to ${styleFile}"
            } else {
                new SLDWriter().write(layer.style)
            }
        } else {
            "Unable to find Layer ${name}"
        }
    }

    @CliCommand(value = "layer buffer", help = "Buffer the input Layer to the output Layer.")
    String buffer(
            @CliOption(key = "input-name", mandatory = true, help = "The Layer name") LayerName inputLayerName,
            @CliOption(key = "output-workspace", mandatory = true, help = "The output Layer Workspace") WorkspaceName workspaceName,
            @CliOption(key = "output-name", mandatory = true, help = "The output Layer name") String outputLayerName,
            @CliOption(key = "distance", mandatory = true, help = "The buffer distance") double distance
    ) throws Exception {
        Layer inputLayer = catalog.layers[inputLayerName]
        if (inputLayer) {
            Workspace outputWorkspace = catalog.workspaces[workspaceName]
            if (outputWorkspace) {
                inputLayer.buffer(distance, outWorkspace: outputWorkspace, outLayer: outputLayerName)
                catalog.layers[new LayerName(outputLayerName)] = outputWorkspace.get(outputLayerName)
                "Done!"
            } else {
                "Unable to find Workspace ${workspaceName}"
            }
        } else {
            "Unable to find Layer ${inputLayerName}"
        }
    }

    @CliCommand(value = "layer centroid", help = "Calculate the centroids of the input Layer to the output Layer.")
    String centroids(
            @CliOption(key = "input-name", mandatory = true, help = "The Layer name") LayerName inputLayerName,
            @CliOption(key = "output-workspace", mandatory = true, help = "The output Layer Workspace") WorkspaceName workspaceName,
            @CliOption(key = "output-name", mandatory = true, help = "The output Layer name") String outputLayerName
    ) throws Exception {
        Layer inputLayer = catalog.layers[inputLayerName]
        if (inputLayer) {
            Workspace outputWorkspace = catalog.workspaces[workspaceName]
            if (outputWorkspace) {
                Schema schema = inputLayer.schema.changeGeometryType("Point", outputLayerName)
                Layer outputLayer = outputWorkspace.create(schema)
                outputLayer.withWriter { LayerWriter writer ->
                    inputLayer.eachFeature { Feature f ->
                        Map values = [:]
                        f.attributes.each { k, v ->
                            if (v instanceof geoscript.geom.Geometry) {
                                values[k] = v.centroid
                            } else {
                                values[k] = v
                            }
                        }
                        writer.add(outputLayer.schema.feature(values, f.id))
                    }
                }
                catalog.layers[new LayerName(outputLayerName)] = outputWorkspace.get(outputLayerName)
                "Done!"
            } else {
                "Unable to find Workspace ${workspaceName}"
            }
        } else {
            "Unable to find Layer ${inputLayerName}"
        }
    }
    
    @CliCommand(value = "layer random", help = "Create a Layer with a number of randomly located points")
    String random(
            @CliOption(key = "output-workspace", mandatory = true, help = "The output Layer Workspace") WorkspaceName workspaceName,
            @CliOption(key = "output-name", mandatory = true, help = "The output Layer name") String outputLayerName,
            @CliOption(key = "number", mandatory = true, help = "The number of points") int numberOfPoints,
            @CliOption(key = "geometry", mandatory = true, help = "The geometry or bounds in which to create the points ") String geometry,
            @CliOption(key = "projection", mandatory = true, help = "The projection") String projection,
            @CliOption(key = "id-field", specifiedDefaultValue = "id", mandatory = false, help = "The id field name") String idFieldName,
            @CliOption(key = "geometry-field", specifiedDefaultValue = "geom", unspecifiedDefaultValue = "geom", mandatory = false, help = "The geometry field name") String geometryFieldName,
            @CliOption(key = "grid", specifiedDefaultValue = "false", unspecifiedDefaultValue = "false", mandatory = false, help = "Whether to create points in a grid")boolean grid,
            @CliOption(key = "constrained-to-circle", specifiedDefaultValue = "false", unspecifiedDefaultValue = "false", mandatory = false, help = "Whether points should be constrained to a circle")boolean constrainedToCircle,
            @CliOption(key = "gutter-fraction", specifiedDefaultValue = "0", unspecifiedDefaultValue = "0", mandatory = false, help = "The size of gutter between cells") int gutterFraction
    ) throws Exception {
      Workspace outputWorkspace = catalog.workspaces[workspaceName]
      if (outputWorkspace) {
          Schema schema = new Schema(outputLayerName, [
            new Field(geometryFieldName, "Point", projection),
            new Field("id","int")
          ])
          Layer outputLayer = outputWorkspace.create(schema)
          outputLayer.withWriter { LayerWriter writer ->
              MultiPoint multiPoint
              if (grid) {
                  multiPoint = Geometry.createRandomPointsInGrid(Geometry.fromString(geometry), numberOfPoints, constrainedToCircle, gutterFraction)
              } else {
                  multiPoint = Geometry.createRandomPoints(Geometry.fromString(geometry), numberOfPoints)
              }
              multiPoint.points.eachWithIndex { Point pt, int i ->
                  Feature f = writer.newFeature
                  Map values = [:]
                  values[geometryFieldName] = pt
                  values["id"] = i
                  f.set(values)
                  writer.add(f)
              }
          }
          catalog.layers[new LayerName(outputLayerName)] = outputWorkspace.get(outputLayerName)
          "Done!"
      }
    }
}

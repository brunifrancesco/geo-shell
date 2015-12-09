package org.geoshell.raster

import geoscript.geom.Geometry
import geoscript.layer.Band
import geoscript.layer.Format
import geoscript.layer.Raster
import geoscript.proj.Projection
import geoscript.style.Style
import geoscript.style.io.CSSReader
import geoscript.style.io.SLDReader
import geoscript.style.io.SLDWriter
import org.geoshell.Catalog
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.shell.core.CommandMarker
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption
import org.springframework.shell.support.util.OsUtils
import org.springframework.stereotype.Component

@Component
class RasterCommands implements CommandMarker {

    @Autowired
    Catalog catalog

    @CliCommand(value = "raster open", help = "Open a Raster.")
    String open(
            @CliOption(key = "format", mandatory = true, help = "The Format name") FormatName formatName,
            @CliOption(key = "raster", mandatory = true, help = "The Raster name") RasterName rasterName,
            @CliOption(key = "name", mandatory = false, help = "The name") String name
    ) throws Exception {
        if (!name) {
            name = "${formatName.name}:${rasterName.name}"
        }
        Format format = catalog.formats[formatName]
        if (format) {
            if (format.names.contains(rasterName.name)) {
                Raster raster = format.read(rasterName.name)
                catalog.rasters[new RasterName(name ?: rasterName.name)] = raster
                "Opened Format ${formatName.name} Raster ${rasterName.name} as ${name}"
            } else {
                "Unable to find Raster ${rasterName}"
            }
        } else {
            "Unable to find Format ${formatName}"
        }
    }

    @CliCommand(value = "raster close", help = "Close a Raster.")
    String close(
            @CliOption(key = "name", mandatory = true, help = "The Raster name") RasterName name
    ) throws Exception {
        Raster raster = catalog.rasters[name]
        if (raster) {
            raster.dispose()
            catalog.rasters.remove(name)
            "Raster ${name} closed!"
        } else {
            "Unable to find Raster ${name}"
        }
    }

    @CliCommand(value = "raster list", help = "List open Rasters.")
    String list() throws Exception {
        catalog.rasters.collect { RasterName name, Raster raster ->
            "${name} = ${raster.format}"
        }.join(OsUtils.LINE_SEPARATOR)
    }

    @CliCommand(value = "raster info", help = "Get information about a Raster.")
    String info(
            @CliOption(key = "name", mandatory = true, help = "The Raster name") RasterName name
    ) throws Exception {
        Raster raster = catalog.rasters[name]
        if (raster) {
            String NEW_LINE = System.getProperty("line.separator")
            StringBuilder builder = new StringBuilder()
            builder.append("Format: ${raster.format ? raster.format.name : 'Unknown'}")
            builder.append(NEW_LINE)
            builder.append("Size: ${raster.size[0]}, ${raster.size[1]}")
            builder.append(NEW_LINE)
            builder.append("Projection ID: ${raster.proj != null ? raster.proj.id : 'Unknown'}")
            builder.append(NEW_LINE)
            builder.append("Projection WKT: ${raster.proj != null ? raster.proj.wkt : 'Unknown'}")
            builder.append(NEW_LINE)
            builder.append("Extent: ${raster.bounds.minX}, ${raster.bounds.minY}, ${raster.bounds.maxX}, ${raster.bounds.maxY}")
            builder.append(NEW_LINE)
            builder.append("Pixel Size: ${raster.pixelSize[0]}, ${raster.pixelSize[1]}")
            builder.append(NEW_LINE)
            builder.append("Block Size: ${raster.blockSize[0]}, ${raster.blockSize[1]}")
            builder.append(NEW_LINE)
            Map extrema = raster.extrema
            builder.append("Bands:")
            raster.bands.eachWithIndex { Band b, int i ->
                builder.append(NEW_LINE)
                builder.append("   ${b}")
                builder.append(NEW_LINE)
                builder.append("      Min Value: ${extrema.min[i]} Max Value: ${extrema.max[i]}")
            }
            builder.toString()
        } else {
            "Unable to find Raster ${name}"
        }
    }

    @CliCommand(value = "raster crop", help = "Crop a Raster.")
    String crop(
            @CliOption(key = "name", mandatory = true, help = "The Raster name") RasterName name,
            @CliOption(key = "output-format", mandatory = true, help = "The output Format Workspace") FormatName formatName,
            @CliOption(key = "output-name", mandatory = false, help = "The output Raster name") String outputRasterName,
            @CliOption(key = "geometry", mandatory = true, help = "The geometry") String geometry
            ) throws Exception {
        Raster raster = catalog.rasters[name]
        if (raster) {
            Format format = catalog.formats[formatName]
            if (format) {
                Raster croppedRaster = raster.crop(Geometry.fromString(geometry))
                format.write(croppedRaster)
                if (!outputRasterName) {
                    outputRasterName = formatName.name
                }
                catalog.rasters[new RasterName(outputRasterName)] = format.read(outputRasterName)
                "Raster ${name} cropped to ${outputRasterName}!"
            } else {
                "Unable to find Raster Format ${formatName}"
            }
        } else {
            "Unable to find Raster ${name}"
        }
    }

    @CliCommand(value = "raster reproject", help = "Project a Raster.")
    String reproject(
            @CliOption(key = "name", mandatory = true, help = "The Raster name") RasterName name,
            @CliOption(key = "output-format", mandatory = true, help = "The output Format Workspace") FormatName formatName,
            @CliOption(key = "output-name", mandatory = false, help = "The output Raster name") String outputRasterName,
            @CliOption(key = "projection", mandatory = true, help = "The projection") String projection
    ) throws Exception {
        Raster raster = catalog.rasters[name]
        if (raster) {
            Format format = catalog.formats[formatName]
            if (format) {
                Raster reprojectedRaster = raster.reproject(new Projection(projection))
                format.write(reprojectedRaster)
                if (!outputRasterName) {
                    outputRasterName = formatName.name
                }
                catalog.rasters[new RasterName(outputRasterName)] = format.read(outputRasterName)
                "Raster ${name} reprojected to ${outputRasterName} as ${projection}!"
            } else {
                "Unable to find Raster Format ${formatName}"
            }
        } else {
            "Unable to find Raster ${name}"
        }
    }

    @CliCommand(value = "raster style set", help = "Set a Raster's style")
    String setStyle(
            @CliOption(key = "name", mandatory = true, help = "The Raster name") RasterName name,
            @CliOption(key = "style", mandatory = true, help = "The SLD or CSS File") File styleFile
    ) throws Exception {
        Raster raster = catalog.rasters[name]
        if (raster) {
            Style style = null
            if (styleFile.name.endsWith(".sld")) {
                style = new SLDReader().read(styleFile)
            } else if (styleFile.name.endsWith(".css")) {
                style = new CSSReader().read(styleFile)
            }
            if (style) {
                raster.style = style
                "Style ${styleFile.absolutePath} set on ${name}"
            } else {
                "Unable to read ${styleFile.absolutePath}"
            }
        } else {
            "Unable to find Raster ${name}"
        }
    }

    @CliCommand(value = "raster style get", help = "Get the Raster's style.")
    String getStyle(
            @CliOption(key = "name", mandatory = true, help = "The Raster name") RasterName name,
            @CliOption(key = "style", mandatory = false, help = "The SLD File") File styleFile
    ) throws Exception {
        Raster raster = catalog.rasters[name]
        if (raster) {
            if (styleFile) {
                new SLDWriter().write(raster.style, styleFile)
                "${name} style written to ${styleFile}"
            } else {
                new SLDWriter().write(raster.style)
            }
        } else {
            "Unable to find Raster ${name}"
        }
    }

}

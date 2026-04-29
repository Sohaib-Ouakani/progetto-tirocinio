package utility

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.parser.Parser

class DescriptionParser(xmlDescription: String) {
    val doc: Document = Ksoup.parse(xmlDescription, parser = Parser.xmlParser())
    val kindElement = doc.selectFirst("CoSimulation, ModelExchange, CoSimulationAndModelExchange")
        ?: error("No CoSimulation/ModelExchange tag found — invalid modelDescription.xml")


    fun findSourceFiles(): List<String> {
        val sourceFiles = kindElement.select("SourceFiles > File")
            .map { it.attr("name") }
            .also { check(it.isNotEmpty()) { "No source files found in <SourceFiles>" } }

        println("KSOUP source files: $sourceFiles")

        return sourceFiles
    }

    fun findModelId(): String {
        val modelId = kindElement.attr("modelIdentifier")
            .also { check(it.isNotBlank()) { "modelIdentifier not found" } }

        println("KSOUP model identifier: $modelId")

        return modelId
    }
}

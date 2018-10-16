This repository contains the following projects.

- "metamodels": Contains the source examples, extracted from the ones
analysed in "When and How to Use Multi-Level Modelling" that we used
so far. Each of them is inside an Eclipse project, and includes the
Ecore metamodel, a visualization file (plus a PNG version) and, in
some cases, example instances of the metamodel. The ones included so
far are:

  - ACME
  - Agate
  - Ant
  - CloudML (3 versions)
  - HAL
  - Matlab
  - SecurityPolicy
  - MatLab/Simulink 

  The folder "annotated" contains copies of some of these metamodels,
  with hand-made annotations.
  The folder "others" contains models that have been checked, but not
  thoroughly examined.

- "MultilevelHierarchy": Eclipse project with the metamodel used to
define the target multilevel hierarchies, plus a visualization file
and a PNG version.

- "Common": Contains shared code, constants and libraries for the
projects listed below.

- "Annotator": Eclipse Java plugin project that can be exported into
a standalone JAR. Used for the automatic annotation of patterns that
can be refactored into multilevel structures in an Ecore metamodel.

- "Transformer": Eclipse Java plugin project that can be exported
into a standalone JAR. Used for the transformation of an annotated
Ecore into a multilevel hierarchy, as an instance of
MultilevelHierarchy.

- "Recommender": Eclipse Java plugin project that can be exported
into a standalone JAR. From a set of registered tools, and based on
the multilevel features of the transformed hierarchy, recommends the
best tool to export the hierarchy to, and offers the full ranking.

- "Exporter": Eclipse Java plugin project that can be exported into a
standalone JAR. Allows to export an instance of MultilevelHierarchy
into any of the registered tools that can support all its multilevel
features.

- "Importer": Eclipse Java plugin project that can be exported into a
standalone JAR. Allows to import from some multilevel tools into an
instance of MultilevelHierarchy.

- "Rearchitecter": Eclipse Java plugin project that can be exported
into a standalone JAR. Encapsulates the whole process of annotating,
transforming, recommending and exporting Ecore models.

* NOTE: All the standalone JARs require a copy of the file
"MultilevelHierarchy.ecore" inside the "MultilevelHierarchy" project
to be in the same folder as them. In order to generate a valid JAR
from the projects' source code, use the "Export" option in Eclipse:
From the Project Explorer, right-click the project > Export... > Java
> Runnable JAR file > Next > Select "Extract required libraries..." >
Set the appropriate JAR name and run configuration (after running it
at least once) > Finish. Click "OK" in the popup and ignore the
warnings at the end.


Related publications:

- Fernando Macías, Esther Guerra and Juan de Lara. “Towards
rearchitecting meta-models into multi-level models”. International
Conference on Conceptual Modeling. Springer. 2017, pp. 59–68

- Fernando Macías, Adrian Rutle and Volker Stolz. "A tool for the
convergence of multilevel modelling approaches". 5th International
Workshop on Multi-Level Modelling (MULTI 2018). 2018.
# HomeFinder - Sistema de Gestión Inmobiliaria (App Móvil) 📱🏢

**HomeFinder**, una aplicación móvil nativa para Android desarrollada. Este sistema está diseñado para la compra y venta de propiedades para una Agencia Inmobiliaria.

## 👤 Autor
* **Nicolás Gerardo Díaz Palacios**

---

## 📊 Estudio de Dominio de Aplicación

El dominio principal de esta aplicación es el sector de **Bienes Raíces (Real Estate)**, enfocado exclusivamente en la **compra y venta de inmuebles** (casas, departamentos, locales comerciales y terrenos). 

Para el diseño funcional y de experiencia de usuario (UX/UI), se analizaron los estándares de las aplicaciones líderes en el mercado inmobiliario regional:
* **ZonaProp:** Se tomó como referencia la jerarquía visual de las tarjetas de propiedades y la claridad en la exposición de precios.
* **ArgenProp:** Se analizó el flujo de filtrado y búsqueda por zonas geográficas.

**Diferenciador:** A diferencia de las plataformas masivas mencionadas (que operan como clasificados abiertos), esta aplicación está concebida como una herramienta cerrada (B2C) para una única inmobiliaria, permitiendo un control total del inventario mediante un rol de Administrador y un canal de comunicación directo y personalizado con los clientes.

---

## 🎨 Prototipo y Diseño de Interfaces (Figma)

El diseño visual de las pantallas, la paleta de colores y el flujo de navegación de la aplicación fueron maquetados previamente para asegurar una buena experiencia de usuario (UX/UI).

🔗 **[Ver el prototipo y diseño en Figma](https://www.figma.com/design/oQ09QgknUuXKlA3Eg7Kmcd/App-Movil-Labo-6?m=auto&t=Go25Cst4ONLrEFhp-6)**

---

## 🛠️ Tecnologías y Arquitectura

El proyecto está construido bajo una arquitectura limpia, separando las responsabilidades de la interfaz de usuario, la lógica de negocio y la persistencia de datos.

* **Entorno de Desarrollo:** Android Studio.
* **Lenguaje:** Java nativo.
* **Servicio Web / BaaS:** [Supabase](https://supabase.com/).
* **Control de Versiones:** Git / GitHub.

---

## 📡 Conexiones y Almacenamiento de Datos

La aplicación utiliza un enfoque híbrido para la gestión de datos, combinando almacenamiento remoto para el catálogo y persistencia local para optimizar la experiencia del usuario.

### 1. Base de Datos Remota (Supabase)
Se implementa una conexión obligatoria a Supabase (PostgreSQL en la nube) para el consumo de la API REST que maneja:
* El catálogo centralizado de propiedades (títulos, descripciones, precios, características).
* **Recursos Externos:** Almacenamiento y recuperación de contenido multimedia (URLs de fotos y videos de los inmuebles).

### 2. Base de Datos Local (Room / SQLite)
Se utiliza para la persistencia de datos en el dispositivo del usuario, garantizando un acceso rápido y reduciendo las peticiones de red:
* **Gestión de Sesión:** Mantenimiento del estado de inicio de sesión (Token/Usuario activo).
* **Favoritos:** Almacenamiento de las propiedades marcadas por el usuario para su rápida visualización offline.
* **Historial de Notificaciones:** Registro local de los avisos recibidos.

---

## 🗄️ Esquemas de Base de Datos

A continuación, se detalla de forma visual la estructura y el modelado de datos utilizado para el funcionamiento del proyecto:

### Modelo de Datos Remoto (Supabase)
*(Sección reservada para el diagrama relacional de la base de datos en la nube)*

### Modelo de Datos Local (Room)
*(Sección reservada para el diagrama de entidades implementado localmente en el dispositivo)*
---

## ✨ Características Principales

* 📍 **Geolocalización (Light):** Integración con mapas para visualizar la ubicación aproximada de los inmuebles, facilitando el reconocimiento de la zona (barrios, puntos de interés).
* 📸 **Soporte Multimedia:** Galerías de imágenes de alta calidad y soporte para recorridos en video de las propiedades.
* 💬 **Integración con WhatsApp:** Botón de acción rápida (Deep Link) que redirige al usuario a un chat de WhatsApp pre-armado con el vendedor, incluyendo la referencia de la propiedad en la que está interesado.
* 🔔 **Notificaciones Push:** Sistema de alertas para mantener informados a los clientes sobre nuevas propiedades cargadas, rebajas de precios o cambios de estado en sus favoritos.
* 🔐 **Gestión de Roles:** Sistema de acceso dual (Administrador para ABM de propiedades vs. Usuario Cliente para exploración del catálogo).

---

## 🚀 Instalación y Ejecución

1. Clonar el repositorio: `git clone https://github.com/nicodiazpalacios/final-lab6-inmoventas.git`
2. Abrir el proyecto en **Android Studio**.
3. Sincronizar el proyecto con los archivos de Gradle.
4. Ejecutar en un emulador o dispositivo físico.
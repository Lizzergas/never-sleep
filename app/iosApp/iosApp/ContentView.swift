import Shared
import SwiftUI
import UIKit

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        if #available(iOS 26.0, *) {
            NativeLiquidGlassAppView()
        } else {
            ComposeView()
                .ignoresSafeArea()
                .onOpenURL { url in
                    _ = AppDeepLinksKt.openAppDeepLink(url: url.absoluteString)
                }
        }
    }
}

struct RouteWrapper: Hashable, Identifiable {
    let id = UUID()
    let route: Navigation3_runtimeNavKey

    static func == (lhs: RouteWrapper, rhs: RouteWrapper) -> Bool {
        lhs.id == rhs.id
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}

@available(iOS 26.0, *)
@MainActor
final class AppNavigationCoordinator: ObservableObject {
    let topLevelDestinations: [NavigationAppDestination]

    @Published var selectedTopLevelId: String
    @Published var pathsByTopLevelId: [String: [RouteWrapper]] = [:]
    @Published var fullScreenRoute: RouteWrapper?

    init() {
        topLevelDestinations = AppDestinationsKt.topLevelDestinations()
        let fallbackDestination = topLevelDestinations.first
        selectedTopLevelId = fallbackDestination?.id ?? ""

        for destination in topLevelDestinations {
            pathsByTopLevelId[destination.id] = []
        }

        let startRoute = MainViewControllerKt.resolveIosStartRouteBlocking()
        let startDestination = AppDestinationsKt.destinationForRoute(route: startRoute)

        if let topLevelDestination = startDestination, topLevelDestination.isTopLevel {
            selectedTopLevelId = topLevelDestination.id
        } else {
            fullScreenRoute = RouteWrapper(route: startRoute)
        }
    }

    func pathBinding(for id: String) -> Binding<[RouteWrapper]> {
        Binding(
            get: { self.pathsByTopLevelId[id] ?? [] },
            set: { self.pathsByTopLevelId[id] = $0 }
        )
    }

    func navigate(to route: Navigation3_runtimeNavKey) {
        if let destination = AppDestinationsKt.destinationForRoute(route: route) {
            if destination.isTopLevel {
                activate(route)
                return
            }
            if destination.isFullScreen {
                fullScreenRoute = RouteWrapper(route: route)
                return
            }
        }

        var path = pathsByTopLevelId[selectedTopLevelId] ?? []
        path.append(RouteWrapper(route: route))
        pathsByTopLevelId[selectedTopLevelId] = path
    }

    func goBack() {
        if fullScreenRoute != nil {
            fullScreenRoute = nil
            return
        }
        var path = pathsByTopLevelId[selectedTopLevelId] ?? []
        guard !path.isEmpty else { return }
        path.removeLast()
        pathsByTopLevelId[selectedTopLevelId] = path
    }

    func set(_ route: Navigation3_runtimeNavKey) {
        guard let destination = AppDestinationsKt.destinationForRoute(route: route) else {
            fullScreenRoute = RouteWrapper(route: route)
            return
        }

        if destination.isTopLevel {
            fullScreenRoute = nil
            selectedTopLevelId = destination.id
            pathsByTopLevelId[destination.id] = []
        } else if destination.isFullScreen {
            fullScreenRoute = RouteWrapper(route: route)
        } else {
            navigate(to: route)
        }
    }

    func activate(_ route: Navigation3_runtimeNavKey) {
        guard let destination = AppDestinationsKt.destinationForRoute(route: route),
              destination.isTopLevel
        else {
            return
        }
        fullScreenRoute = nil
        selectedTopLevelId = destination.id
    }

    func apply(command: IosNavigationCommand) {
        guard !command.stack.isEmpty else { return }

        if command.isFullScreen {
            fullScreenRoute = RouteWrapper(route: command.stack.last!)
            return
        }

        guard let destination = AppDestinationsKt.destinationForRoute(route: command.selectedTopLevelRoute),
              destination.isTopLevel
        else {
            return
        }

        fullScreenRoute = nil
        selectedTopLevelId = destination.id
        pathsByTopLevelId[destination.id] = command.stack.dropFirst().map { RouteWrapper(route: $0) }
    }

    func title(for route: Navigation3_runtimeNavKey) -> String {
        AppDestinationsKt.destinationForRoute(route: route)?.title ?? ""
    }

    func usesLargeTitle(for route: Navigation3_runtimeNavKey) -> Bool {
        AppDestinationsKt.destinationForRoute(route: route)?.usesLargeTitle ?? false
    }
}

@available(iOS 26.0, *)
struct NativeLiquidGlassAppView: View {
    @StateObject private var coordinator = AppNavigationCoordinator()

    var body: some View {
        Group {
            if coordinator.fullScreenRoute == nil {
                tabShell
            } else {
                Color.clear
                    .accessibilityHidden(true)
            }
        }
        .onOpenURL { url in
            if let command = IosNavigationBridgeKt.nativeDeepLinkCommand(url: url.absoluteString) {
                coordinator.apply(command: command)
            }
        }
        .fullScreenCover(item: $coordinator.fullScreenRoute) { wrapper in
            NativeComposeRouteView(
                route: wrapper.route,
                appCoordinator: coordinator
            )
            .ignoresSafeArea()
        }
    }

    private var tabShell: some View {
        TabView(selection: $coordinator.selectedTopLevelId) {
            ForEach(coordinator.topLevelDestinations, id: \.id) { destination in
                Tab(
                    destination.title,
                    systemImage: destination.systemImage ?? "circle",
                    value: destination.id
                ) {
                    NativeTabRootView(
                        destination: destination,
                        appCoordinator: coordinator
                    )
                }
            }
        }
        .tint(Color("AccentColor"))
        .tabBarMinimizeBehavior(.automatic)
    }
}

@available(iOS 26.0, *)
struct NativeTabRootView: View {
    let destination: NavigationAppDestination
    @ObservedObject var appCoordinator: AppNavigationCoordinator

    var body: some View {
        NavigationStack(
            path: appCoordinator.pathBinding(for: destination.id)
        ) {
            NativeComposeRouteView(
                route: destination.route,
                appCoordinator: appCoordinator
            )
            .navigationTitle(destination.title)
            .toolbarTitleDisplayMode(destination.usesLargeTitle ? .large : .inline)
            .navigationDestination(for: RouteWrapper.self) { wrapper in
                NativeComposeRouteView(
                    route: wrapper.route,
                    appCoordinator: appCoordinator
                )
                .navigationTitle(appCoordinator.title(for: wrapper.route))
                .toolbarTitleDisplayMode(
                    appCoordinator.usesLargeTitle(for: wrapper.route) ? .large : .inline
                )
            }
        }
    }
}

@available(iOS 26.0, *)
struct NativeComposeRouteView: UIViewControllerRepresentable {
    let route: Navigation3_runtimeNavKey
    @ObservedObject var appCoordinator: AppNavigationCoordinator

    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.ScreenViewController(
            route: route,
            onNavigate: { route in appCoordinator.navigate(to: route) },
            onGoBack: { appCoordinator.goBack() },
            onSet: { route in appCoordinator.set(route) },
            onActivate: { route in appCoordinator.activate(route) }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

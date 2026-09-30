# Changelog

All notable changes to this project are documented in this file.

## [0.1.0] - 2026-04-29

### Added
- Initial release of HeatSync for Forge `1.20.1`.
- Cold Sweat block temperature bridge registration for native Better Industrial Heat heat pipes.
- Pipe thermal update controller that applies ambient blending, network equalization, passive loss, and nearby cold-source pull.
- Configurable thermal mapping and behavior via Forge common config.
- Block tags for `better_industrial_heat:pipe_cold_sources` and `better_industrial_heat:pipe_radiators`.
- Unit tests for mapping math and thermal step behavior.

## Unreleased

- Reconciled dependency and user-facing terminology with Better Industrial Heat's native transport ownership; retired Create: New Age, Alchemylib, and Alchemistry are not runtime dependencies.
- Normalize the project identity to `better-industrial-heat / better_industrial_heat (formerly heatsync)`; this is a clean break with no legacy aliases or migrations.
- Allow otherwise-identical food stacks with different thermal state to merge, recomputing temperature and decay by item-count-weighted averaging while preserving the ordinary item identity and all non-thermal data.
- Emit optional correlated Threads evidence when frozen food is rejected and the same ordinary item is later consumed thawed and fresh.
- Keep spoiled-food Thirst and Diet drains server-authoritative, preventing client-side capability mutation and a Thirst sync crash.
- Require actively fueled Blaze Burners for Create boiler heat and bulk blasting; passive heaters and lava blasting no longer provide infinite heat.
